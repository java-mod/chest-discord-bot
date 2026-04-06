package com.example.chestbot.service;

import com.example.chestbot.dto.BankLogRow;
import com.example.chestbot.dto.ChestLogRow;
import com.example.chestbot.persistence.entity.ChestLogEntity;
import com.example.chestbot.persistence.entity.IslandBankLogEntity;
import com.example.chestbot.persistence.repository.ChestLogRepository;
import com.example.chestbot.persistence.repository.IslandBankLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IslandDashboardService {

    private static final int PAGE_SIZE   = 30;
    private static final int MAX_EXPORT  = 5000;

    private final ChestLogRepository chestLogRepository;
    private final IslandBankLogRepository islandBankLogRepository;

    public IslandDashboardService(ChestLogRepository chestLogRepository,
                                   IslandBankLogRepository islandBankLogRepository) {
        this.chestLogRepository = chestLogRepository;
        this.islandBankLogRepository = islandBankLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<ChestLogRow> getChestLogs(Long islandId, int page,
                                           String playerName, String chestKey) {
        int safePage = Math.max(0, page);
        String pn = blank(playerName);
        String ck = blank(chestKey);
        return chestLogRepository
                .searchByIslandId(islandId, pn, ck, PageRequest.of(safePage, PAGE_SIZE))
                .map(ChestLogRow::from);
    }

    @Transactional(readOnly = true)
    public Page<BankLogRow> getBankLogs(Long islandId, int page,
                                         String playerName, String transactionType) {
        int safePage = Math.max(0, page);
        String pn = blank(playerName);
        String tt = blank(transactionType);
        return islandBankLogRepository
                .searchByIslandId(islandId, pn, tt, PageRequest.of(safePage, PAGE_SIZE))
                .map(BankLogRow::from);
    }

    @Transactional(readOnly = true)
    public String buildChestLogExport(Long islandId, String playerName, String chestKey) {
        String pn = blank(playerName);
        String ck = blank(chestKey);
        List<ChestLogEntity> rows = chestLogRepository
                .searchByIslandId(islandId, pn, ck, PageRequest.of(0, MAX_EXPORT))
                .getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("시간\t플레이어\t창고\t가져간 아이템\t넣은 아이템\n");
        sb.append("=".repeat(80)).append("\n");
        for (ChestLogEntity e : rows) {
            ChestLogRow r = ChestLogRow.from(e);
            sb.append(r.getCreatedAt()).append("\t")
              .append(r.getPlayerName()).append("\t")
              .append(r.getChestKey()).append("\t")
              .append(r.getTakenSummary()).append("\t")
              .append(r.getAddedSummary()).append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String buildBankLogExport(Long islandId, String playerName, String transactionType) {
        String pn = blank(playerName);
        String tt = blank(transactionType);
        List<IslandBankLogEntity> rows = islandBankLogRepository
                .searchByIslandId(islandId, pn, tt, PageRequest.of(0, MAX_EXPORT))
                .getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("시간\t플레이어\t유형\t금액\t사유\n");
        sb.append("=".repeat(80)).append("\n");
        for (IslandBankLogEntity e : rows) {
            BankLogRow r = BankLogRow.from(e);
            sb.append(r.getCreatedAt()).append("\t")
              .append(r.getPlayerName()).append("\t")
              .append(r.getTransactionLabel()).append("\t")
              .append(r.getAmount()).append("\t")
              .append(r.getNote()).append("\n");
        }
        return sb.toString();
    }

    /** 빈 문자열을 null 로 변환 (JPQL null-check 정상 동작을 위해) */
    private static String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
