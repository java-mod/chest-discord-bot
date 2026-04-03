package com.example.chestbot.discord;

import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.service.IslandService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SlashCommandListener.class);

    private final IslandService islandService;

    public SlashCommandListener(IslandService islandService) {
        this.islandService = islandService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("창고")) return;

        String sub = event.getSubcommandName();
        if (sub == null) return;

        switch (sub) {
            case "설정"      -> handleSetup(event);
            case "섬이름"    -> handleIslandName(event);
            case "채널연결"   -> handleBindChannel(event);
            case "코드"      -> handleShowCode(event);
            case "코드재발급" -> handleRegenerateCode(event);
            case "관리자코드" -> handleAdminCode(event);
        }
    }

    private void handleSetup(SlashCommandInteractionEvent event) {
        if (!isAdmin(event)) return;
        event.deferReply(true).queue();

        String islandName = event.getOption("섬이름", OptionMapping::getAsString);
        String guildId   = event.getGuild().getId();
        String guildName = event.getGuild().getName();

        try {
            IslandEntity island = islandService.setupIsland(guildId, guildName, islandName);
            event.getHook().sendMessage(
                    "✅ 섬 **" + island.getName() + "** 설정 완료!\n" +
                    "📌 참여 코드: `" + island.getJoinCode() + "`\n" +
                    "이 코드를 플레이어들에게 공유하고, `/창고 채널연결` 로 창고/은행 로그 채널을 각각 지정하세요."
            ).queue();
        } catch (Exception e) {
            log.error("섬 설정 오류", e);
            event.getHook().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }

    private void handleIslandName(SlashCommandInteractionEvent event) {
        if (!isAdmin(event)) return;
        event.deferReply(true).queue();

        String joinCode = event.getOption("참여코드", OptionMapping::getAsString);
        String islandName = event.getOption("이름", OptionMapping::getAsString);

        try {
            IslandEntity island = islandService.updateIslandDisplayNameByJoinCode(event.getGuild().getId(), joinCode, islandName);
            event.getHook().sendMessage(
                    "✅ 참여 코드 `" + island.getJoinCode() + "`의 표시 이름을 **" + island.getName() + "**(으)로 변경했습니다.\n" +
                    "참여 코드/관리자 코드 흐름은 그대로 유지됩니다."
            ).queue();
        } catch (Exception e) {
            log.error("섬 이름 변경 오류", e);
            event.getHook().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }

    private void handleBindChannel(SlashCommandInteractionEvent event) {
        if (!isAdmin(event)) return;
        event.deferReply(true).queue();

        GuildChannelUnion channel = event.getOption("채널", OptionMapping::getAsChannel);
        String purpose = event.getOption("종류", OptionMapping::getAsString);
        String guildId = event.getGuild().getId();

        try {
            IslandEntity island = islandService.getIslandByGuildDiscordId(guildId);
            String normalizedPurpose = islandService.normalizeChannelPurpose(purpose);
            islandService.bindChannel(island.getId(), channel.getId(), normalizedPurpose);
            String purposeLabel = switch (normalizedPurpose) {
                case IslandService.BANK_LOG_PURPOSE -> "섬 은행 기록";
                default -> "창고 로그";
            };
            event.getHook().sendMessage(
                    "✅ <#" + channel.getId() + "> 채널을 **" + island.getName() + "** " + purposeLabel + " 채널로 설정했습니다."
            ).queue();
        } catch (Exception e) {
            log.error("채널 연결 오류", e);
            event.getHook().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }

    private void handleShowCode(SlashCommandInteractionEvent event) {
        if (!isAdmin(event)) return;
        event.deferReply(true).queue();

        try {
            IslandEntity island = islandService.getIslandByGuildDiscordId(event.getGuild().getId());
            String code = island.getJoinCode();
            if (code == null) {
                event.getHook().sendMessage("⚠️ 참여 코드가 없습니다. `/창고 코드재발급` 을 실행하세요.").queue();
            } else {
                event.getHook().sendMessage(
                        "📌 **" + island.getName() + "** 참여 코드: `" + code + "`"
                ).queue();
            }
        } catch (Exception e) {
            event.getHook().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }

    private void handleRegenerateCode(SlashCommandInteractionEvent event) {
        if (!isAdmin(event)) return;
        event.deferReply(true).queue();

        try {
            String newCode = islandService.regenerateJoinCode(event.getGuild().getId());
            event.getHook().sendMessage(
                    "🔄 참여 코드가 재발급되었습니다.\n📌 새 코드: `" + newCode + "`\n기존 코드로 연결된 플레이어는 재입력이 필요합니다."
            ).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }

    private void handleAdminCode(SlashCommandInteractionEvent event) {
        if (!isAdmin(event)) return;
        event.deferReply(true).queue();

        try {
            String code = islandService.generateAdminCode(event.getGuild().getId());
            event.getHook().sendMessage(
                    "🔑 인게임 관리자 코드 (10분 유효, 1회용): `" + code + "`\n" +
                    "인게임에서: `/창고봇 관리자 " + code + "` 을 입력한 뒤, `/창고봇 추가 <이름>` 후 상자를 클릭하세요."
            ).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }

    private boolean isAdmin(SlashCommandInteractionEvent event) {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.reply("❌ 서버 관리자 권한이 필요합니다.").setEphemeral(true).queue();
            return false;
        }
        return true;
    }
}
