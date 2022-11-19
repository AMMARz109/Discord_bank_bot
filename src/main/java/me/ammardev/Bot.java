package me.ammardev;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Scanner;

public class Bot {

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        String token = scanner.nextLine();
        JDABuilder builder = JDABuilder
                .createDefault(token)
                .setEnabledIntents(GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .disableCache(CacheFlag.VOICE_STATE,
                        CacheFlag.EMOJI,
                        CacheFlag.STICKER,
                        CacheFlag.SCHEDULED_EVENTS)
                .addEventListeners(new CommandManger())
                .setActivity(Activity.watching("Bank"));




        JDA jda = builder.build().awaitReady();

        jda.upsertCommand("balance", "Check your bank balance").queue();
        jda.upsertCommand("daily", "Daily bonus").queue();
        jda.upsertCommand("shop", "Bank shop").queue();
        jda.upsertCommand("timeout", "timeout someone for 3 minutes").addOption(OptionType.MENTIONABLE, "target", "target", true).queue();

    }
}
