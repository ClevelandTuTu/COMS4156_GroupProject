package com.project.airhotel.service.core;

import com.project.airhotel.model.enums.ReservationStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ReservationStatusMachine {

  private final Map<ReservationStatus, Set<ReservationStatus>> allowed = new EnumMap<>(ReservationStatus.class);

  public ReservationStatusMachine() {
    allowed.put(ReservationStatus.PENDING, EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELED));
    allowed.put(ReservationStatus.CONFIRMED, EnumSet.of(ReservationStatus.CHECKED_IN, ReservationStatus.CANCELED));
    allowed.put(ReservationStatus.CHECKED_IN, EnumSet.of(ReservationStatus.CHECKED_OUT));
    allowed.put(ReservationStatus.CHECKED_OUT, EnumSet.noneOf(ReservationStatus.class));
    allowed.put(ReservationStatus.CANCELED, EnumSet.noneOf(ReservationStatus.class));
    allowed.put(ReservationStatus.NO_SHOW, EnumSet.noneOf(ReservationStatus.class));
  }

  public boolean canTransit(ReservationStatus from, ReservationStatus to) {
    Set<ReservationStatus> next = allowed.get(from);
    return next != null && next.contains(to);
  }
}
