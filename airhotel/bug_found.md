Area: ReservationStatusService.changeStatus
Symptom: Status history rows were written with reservationId taken from the in-memory entity rather than the persisted entity. If the DB generated/changed the id (or we operated on a detached copy), the history row could point to the wrong reservation.

How we caught it: Added test ReservationStatusServiceTest#changeStatus_historyUsesSavedId that asserts the history row uses the id from the saved entity.

Before:
final Reservations saved = reservationsRepository.save(r);
ReservationsStatusHistory h = new ReservationsStatusHistory();
// ...
h.setReservationId(r.getId()); // could be stale (pre-save id or detached)
historyRepository.save(h);

After:
final Reservations saved = reservationsRepository.save(r);
ReservationsStatusHistory h = new ReservationsStatusHistory();
// ...
h.setReservationId(saved.getId()); // use persisted id
historyRepository.save(h);

Verification

ReservationStatusServiceTest#changeStatus_historyUsesSavedId passes.

Confirmed illegal transitions still throw and no persistence occurs (changeStatus_illegalTransition_throws_noSideEffects).

Notes

Rationale: Always source foreign keys from the persisted aggregate to avoid mismatches with DB-assigned identifiers.

Optional hardening: consider mapping ReservationsStatusHistory with a real relationship (@ManyToOne) to Reservations to leverage JPA’s identity handling and reduce manual fk mistakes.