package me.ammardev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandManger extends ListenerAdapter {


    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        addMember(event.getMember(), event.getGuild());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Guild guild = member.getGuild();
        String memberName = member.getNickname() == null? member.getEffectiveName(): member.getNickname();
        if (event.getName().equals("balance")){
            if (!inBank(member, guild)){
                addMember(member, event.getGuild());
            }
            long balance = check_for_balance(member, guild);

            String name = member.getNickname() != null? member.getNickname(): member.getEffectiveName();

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(new Color(0, 255, 167))
                    .setTitle("\uD835\uDC01\uD835\uDC00\uD835\uDC0D\uD835\uDC0A")
                    .setDescription(String.format("%s your balance is: %d$", name, balance));

            event.replyEmbeds(embed.build()).queue();
        }

        if (event.getName().equals("daily")){
            Button button = Button.success("daily", "Get your daily")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCB0"));
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(new Color(0, 255, 167))
                    .setTitle("\uD835\uDC01\uD835\uDC00\uD835\uDC0D\uD835\uDC0A")
                    .setDescription("get your daily reward");

            event.replyEmbeds(embed.build()).setActionRow(button).queue();
        }

        if (event.getName().equals("shop")){

            List<Button> buttons = new ArrayList<>();

            Button timeout = Button.danger("timeout", "timeout someone for [3min]\n2500$")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCA5"));
            buttons.add(timeout);



            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(new Color(0, 255, 167))
                    .setTitle("\uD835\uDD4A???\uD835\uDD46???")
                    .setDescription("Spend your money here!");

            event.replyEmbeds(embed.build()).setActionRow(buttons).queue();
        }

        if (event.getName().equals("timeout")){
            try {
                Member target = event.getOption("target").getAsMember();


                if (event.getOption("target") instanceof Role || target == null){
                    event.reply("You can't mention roles!").setEphemeral(true).queue();
                    return;
                }
                String targetName = target.getNickname() == null? target.getEffectiveName(): target.getNickname();
                if (target.isOwner()){
                    event.reply("You can't punish owner!").setEphemeral(true).queue();
                    return;
                }

                if (check_for_balance(member, guild) < 2500){
                    event.reply("You don't have enough money!")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                if (member.getVoiceState() != null){
                    if (target.getVoiceState().isGuildMuted()){
                        event.reply("User already muted").setEphemeral(true).queue();
                        return;
                    }
                }

                if (!event.getGuild().getMembers().contains(target)){
                    event.reply("User not found!").setEphemeral(true).queue();
                    return;
                }


                target.timeoutFor(Duration.ofMinutes(3)).submit();
                edit_balance(member, guild, -2500);
                event.reply(String.format("@%s timeouted @%s", memberName, targetName)).queue();
            }catch (Exception e){
                e.printStackTrace();
                event.reply("Failed to mute user, he is to powerful").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        switch (event.getButton().getId()){
            case "daily":{
                if (!isPeriodReady(member, guild)){
                    event.reply("You already claimed your daily today!").setEphemeral(true).queue();
                    return;
                }
                edit_balance(member, guild, +300);
                startPeriod(member, guild);
                event.reply("You took the daily reward!\n+300").setEphemeral(true).queue();
            }
            case "timeout":{
                event.reply("Use command /timeout @target_name")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    void edit_balance(Member member, Guild guild, long amount){
        try {
            String id = member.getId();

            List<BankMember> members = loadMembers(guild);
            for (BankMember m: members) {
                if (m.getId().equals(id)){
                    m.setBal(m.getBal() + amount);
                }
            }
            File dir = Paths.get(Statics.bankPath + guild.getName() + " - " + guild.getId()).toFile();
            File file = Paths.get(dir.getAbsolutePath() + "/bank.json").toFile();
            if (!dir.exists()){
                dir.mkdirs();
                file.createNewFile();
            }
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            try (Writer writer = new FileWriter(file)){
                writer.write(gson.toJson(members.toArray()));
                writer.flush();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean inBank(Member member, Guild guild){
        for (BankMember m: loadMembers(guild)) {
            if (m.getId().equals(member.getId())){
                return true;
            }
        }
        return false;
    }

    long check_for_balance(Member member, Guild guild){
        String id = member.getId();
        List<BankMember> members = loadMembers(guild);
        for (BankMember m: members) {
            if (m.getId().equals(id)){
                return m.getBal();
            }
        }
        return 0;
    }


    void addMember(Member member, Guild guild){
        try {
            String id = member.getId();
            long balance = 300L;

            BankMember bankMember = new BankMember(id, balance);

            List<BankMember> members = loadMembers(guild);
            for (BankMember m: members) {
                if (m.getId().equals(id)){
                    return;
                }
            }
            members.add(bankMember);
            File dir = Paths.get(Statics.bankPath + guild.getName() + " - " + guild.getId()).toFile();
            File file = Paths.get(dir.getAbsolutePath() + "/bank.json").toFile();
            if (!dir.exists()){
                dir.mkdirs();
                file.createNewFile();
            }
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            try (Writer writer = new FileWriter(file)){
                writer.write(gson.toJson(members.toArray()));
                writer.flush();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List <BankMember> loadMembers(Guild guild){
        try {
            File dir = Paths.get(Statics.bankPath + guild.getName() + " - " + guild.getId()).toFile();
            File file = Paths.get(dir.getAbsolutePath() + "/bank.json").toFile();
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            BankMember[] members = gson.fromJson(new FileReader(file), BankMember[].class);
            return new ArrayList<>(List.of(members));
        } catch (FileNotFoundException e) {
            System.out.println("Returned new array");
            return new ArrayList<>();
        }
    }

    void startPeriod(Member member, Guild guild){
        try {
            LocalDateTime current_time = LocalDateTime.now();


            String id = member.getId();
            boolean found = false;
            List<Period> periods = loadPeriods(guild);
            for (Period p: periods){
                if (p.getId().equals(id)){
                    p.setTime(current_time.getDayOfYear());
                    found = true;
                    break;
                }
            }

            if (!found){
                Period period = new Period(id, current_time.getDayOfYear());
                periods.add(period);
            }

            File dir = Paths.get(Statics.bankPath + guild.getName() + " - " + guild.getId()).toFile();
            File file = Paths.get(dir.getAbsolutePath() + "/periods.json").toFile();

            if (!dir.exists()){
                dir.mkdirs();
                file.createNewFile();
            }
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            try (Writer writer = new FileWriter(file)){
                writer.write(gson.toJson(periods.toArray()));
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    List <Period> loadPeriods(Guild guild){
        try {
            File dir = Paths.get(Statics.bankPath + guild.getName() + " - " + guild.getId()).toFile();
            File file = Paths.get(dir.getAbsolutePath() + "/periods.json").toFile();
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            Period[] periods = gson.fromJson(new FileReader(file), Period[].class);
            return new ArrayList<>(List.of(periods));
        } catch (FileNotFoundException e) {
            System.out.println("Returned new array");
            return new ArrayList<>();
        }
    }

    boolean isPeriodReady(Member member, Guild guild){
        for (Period period: loadPeriods(guild)){
            if (period.getId().equals(member.getId())){
                return period.getTime() != LocalDateTime.now().getDayOfYear();
            }
        }
        return true;
    }


}
