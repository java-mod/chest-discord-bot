package com.example.chestbot.service;

import com.example.chestbot.discord.SlashCommandListener;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Currency;
import java.util.Locale;

@Service
public class DiscordService {

    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);
    private static final int EMBED_FIELD_LIMIT = 1000;
    private static final Locale KOREA_LOCALE = Locale.KOREA;

    @Value("${discord.token:}")
    private String token;

    private final SlashCommandListener slashCommandListener;

    private JDA jda;

    public DiscordService(SlashCommandListener slashCommandListener) {
        this.slashCommandListener = slashCommandListener;
    }

    @PostConstruct
    public void init() {
        if (token == null || token.isBlank()) {
            log.warn("DISCORD_TOKEN 미설정 — Discord 기능을 비활성화합니다");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(EnumSet.of(GatewayIntent.GUILD_MESSAGES))
                    .addEventListeners(slashCommandListener)
                    .build();
            jda.awaitReady();
            log.info("Discord 봇 준비 완료: {}", jda.getSelfUser().getName());
            registerSlashCommands();
        } catch (Exception e) {
            log.error("Discord 봇 초기화 실패", e);
        }
    }

    private void registerSlashCommands() {
        jda.updateCommands().addCommands(
                Commands.slash("창고", "창고 관리")
                        .addSubcommands(
                                new SubcommandData("설정", "섬 등록 및 참여 코드 발급")
                                        .addOptions(new OptionData(OptionType.STRING, "섬이름", "섬 이름", true)),
                                new SubcommandData("섬이름", "섬 표시 이름 변경")
                                        .addOptions(
                                                new OptionData(OptionType.STRING, "참여코드", "섬 참여 코드", true),
                                                new OptionData(OptionType.STRING, "이름", "새로 표시할 섬 이름", true)
                                        ),
                                new SubcommandData("채널연결", "창고 또는 섬 은행 로그를 보낼 채널 지정")
                                        .addOptions(
                                                new OptionData(OptionType.STRING, "종류", "연결할 로그 종류", true)
                                                        .addChoice("창고 로그", IslandService.CHEST_LOG_PURPOSE)
                                                        .addChoice("섬 은행 기록", IslandService.BANK_LOG_PURPOSE),
                                                new OptionData(OptionType.CHANNEL, "채널", "로그 채널", true)
                                                        .setChannelTypes(ChannelType.TEXT)
                                        ),
                                new SubcommandData("코드", "현재 참여 코드 조회"),
                                new SubcommandData("코드재발급", "참여 코드 재발급 (기존 코드 무효화)"),
                                new SubcommandData("관리자코드", "인게임 chest 등록용 1회성 코드 발급 (10분 유효)")
                        )
        ).queue(
                ok -> log.info("슬래시 커맨드 등록 완료"),
                err -> log.error("슬래시 커맨드 등록 실패: {}", err.getMessage())
        );
    }

    public void sendChestLogEmbed(String discordChannelId, String islandName, String playerName, String chestKey,
                                  String joinedTaken, String joinedAdded, Instant timestamp) {
        if (jda == null) {
            log.warn("JDA 미초기화 — 메시지 전송 실패");
            return;
        }

        TextChannel channel = jda.getTextChannelById(discordChannelId);
        if (channel == null) {
            log.warn("채널 ID {} 를 찾을 수 없음", discordChannelId);
            return;
        }

        MessageEmbed embed = new EmbedBuilder()
                .setColor(resolveChestLogColor(joinedTaken, joinedAdded))
                .setTitle("📦 창고 로그")
                .setDescription("**" + islandName + "** 섬의 창고 변경 내역입니다.")
                .addField("플레이어", safe(playerName), true)
                .addField("창고", safe(chestKey), true)
                .addField("꺼냄", limitField(safe(joinedTaken)), false)
                .addField("넣음", limitField(safe(joinedAdded)), false)
                .setTimestamp(timestamp)
                .build();

        channel.sendMessageEmbeds(embed).queue(
                ok -> log.info("Discord 전송 완료: channel={}", discordChannelId),
                err -> log.error("Discord 전송 실패: {}", err.getMessage())
        );
    }

    public void sendIslandBankLogEmbed(String discordChannelId, String islandName, String playerName,
                                       String transactionType, long amount, Long balanceAfter,
                                       String note, Instant timestamp) {
        if (jda == null) {
            log.warn("JDA 미초기화 — 메시지 전송 실패");
            return;
        }

        TextChannel channel = jda.getTextChannelById(discordChannelId);
        if (channel == null) {
            log.warn("채널 ID {} 를 찾을 수 없음", discordChannelId);
            return;
        }

        MessageEmbed embed = new EmbedBuilder()
                .setColor(resolveIslandBankLogColor(transactionType))
                .setTitle("🏦 섬 은행 기록")
                .setDescription("**" + islandName + "** 섬의 은행 입출금 내역입니다.")
                .addField("플레이어", safe(playerName), true)
                .addField("유형", resolveBankTransactionLabel(transactionType), true)
                .addField("금액", formatWon(amount), true)
                .addField("사유", limitField(safe(note)), false)
                .setTimestamp(timestamp)
                .build();

        if (balanceAfter != null) {
            embed = new EmbedBuilder(embed)
                    .addField("잔액", formatWon(balanceAfter), true)
                    .build();
        }

        channel.sendMessageEmbeds(embed).queue(
                ok -> log.info("Discord 은행 로그 전송 완료: channel={}", discordChannelId),
                err -> log.error("Discord 은행 로그 전송 실패: {}", err.getMessage())
        );
    }

    private Color resolveChestLogColor(String taken, String added) {
        boolean hasTaken = taken != null && !taken.isBlank() && !taken.equals("없음");
        boolean hasAdded = added != null && !added.isBlank() && !added.equals("없음");
        if (hasTaken && hasAdded) return new Color(0xF1C40F);
        if (hasTaken) return new Color(0xE74C3C);
        if (hasAdded) return new Color(0x2ECC71);
        return new Color(0x95A5A6);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "없음" : value;
    }

    private Color resolveIslandBankLogColor(String transactionType) {
        String normalized = transactionType == null ? "" : transactionType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DEPOSIT", "입금" -> new Color(0x2ECC71);
            case "WITHDRAW", "출금" -> new Color(0xE74C3C);
            default -> new Color(0x3498DB);
        };
    }

    private String resolveBankTransactionLabel(String transactionType) {
        String normalized = transactionType == null ? "" : transactionType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DEPOSIT" -> "입금";
            case "WITHDRAW" -> "출금";
            default -> safe(transactionType);
        };
    }

    private String limitField(String value) {
        if (value.length() <= EMBED_FIELD_LIMIT) {
            return value;
        }
        return value.substring(0, EMBED_FIELD_LIMIT - 4) + "\n...";
    }

    private String formatWon(long amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(KOREA_LOCALE);
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(0);
        formatter.setCurrency(Currency.getInstance("KRW"));
        return formatter.format(amount) + "원";
    }
}
