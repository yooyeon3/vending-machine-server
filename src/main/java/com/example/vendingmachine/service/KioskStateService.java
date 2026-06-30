package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class KioskStateService {

    private final AtomicBoolean kioskBusy = new AtomicBoolean(false);
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public KioskStateService(PurchaseHistoryRepository purchaseHistoryRepository) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    public boolean isBusy() {
        return kioskBusy.get();
    }

    public void setBusy() {
        kioskBusy.set(true);
    }

    public void setFree() {
        kioskBusy.set(false);
        // RESERVED → PENDING 승격 (예약 대기 주문 즉시 처리 시작)
        List<PurchaseHistory> reserved = purchaseHistoryRepository
                .findByDeliveryStatus(PurchaseHistory.DeliveryStatus.RESERVED);
        for (PurchaseHistory h : reserved) {
            h.setDeliveryStatus(PurchaseHistory.DeliveryStatus.PENDING);
            purchaseHistoryRepository.save(h);
        }
    }
}
