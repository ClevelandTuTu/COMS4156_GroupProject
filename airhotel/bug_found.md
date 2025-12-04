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

**Additional bugs found:**
- In Authentication, sign-in was being done through a custom html page and then going to Google Sign-in Page.
    - When introducing a client program, we wanted a way for them to sign in and sign-out directly from the client program to our endpoints.
    - This has now been fixed.
        - Custom login and logout pages are removed.
        - Redirect URL on successful sign-in now goes straight back to Client Program.
```Java
.oauth2Login(oauth2 -> oauth2
    .successHandler(this::oauthSuccessHandler)
)

private void oauthSuccessHandler(
    final jakarta.servlet.http.HttpServletRequest req,
    final jakarta.servlet.http.HttpServletResponse res,
    final Authentication authentication
) throws IOException {

  OAuth2User principal = (OAuth2User) authentication.getPrincipal();
  String email = principal.getAttribute("email");
  String name  = principal.getAttribute("name");

  // create/find local user
  Long userId = authUserService.findOrCreateByEmail(email, name);

  // ensure session exists and store user id
  var session = req.getSession(true);
  session.setAttribute(SESSION_USER_ID, userId);

  String redirectUrl = "http://localhost:5173/";
  res.sendRedirect(redirectUrl);
}  
```
- Also, added the following properties to fix the issue of multiple jsession cookie ids being set by creating a custom cookie name.
```java
server.servlet.session.cookie.name=AIRHOTELSESSION
server.servlet.session.cookie.same-site=none
server.servlet.session.cookie.secure=true
```

- In public API endpoints for searching.
    - We have found bugs in our search functionality when searching by city.
        - The sql was running strict type matching.
        - We have introduced like operation for pattern match search.