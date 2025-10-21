package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ReservationOrchestrator {
  private final ReservationInventoryService inventoryService;
  private final ReservationStatusService statusService;

  /** 取消预订：释放库存 -> 记录取消时间 -> 状态变更 todo: 释放库存/退款等后续再补 */
  @Transactional
  public void cancel(Reservations r, String reason, Long changedByUserId) {
    if (r.getStatus() == ReservationStatus.CANCELED) return;
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      throw new BadRequestException("Reservation already checked out and cannot be cancelled now.");
    }

    // 释放库存（注意：如果是经理端取消，用户端取消，同逻辑）
    inventoryService.releaseRange(
        r.getHotel_id(), r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date()
    );

    r.setCanceled_at(LocalDateTime.now());
    statusService.changeStatus(r, ReservationStatus.CANCELED, reason, changedByUserId);
  }
}
