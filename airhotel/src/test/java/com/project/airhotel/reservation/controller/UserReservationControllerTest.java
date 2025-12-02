package com.project.airhotel.reservation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.airhotel.reservation.dto.CreateReservationRequest;
import com.project.airhotel.reservation.dto.PatchReservationRequest;
import com.project.airhotel.reservation.dto.ReservationDetailResponse;
import com.project.airhotel.reservation.dto.ReservationSummaryResponse;
import com.project.airhotel.reservation.service.UserReservationService;
import com.project.airhotel.user.service.AuthUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class UserReservationControllerTest {

  @Mock
  private UserReservationService service;

  @Mock
  private AuthUserService authUserService;

  @Mock
  private Authentication authentication;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpSession session;

  @Mock
  private OAuth2User principal;

  @InjectMocks
  private UserReservationController controller;

  @Test
  @DisplayName("list uses session user id when present; does not call authUserService")
  void list_usesSessionId() {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(any())).thenReturn(99L);
    final ReservationSummaryResponse dto = new ReservationSummaryResponse();
    when(service.listMyReservations(99L)).thenReturn(List.of(dto));

    final List<ReservationSummaryResponse> out = controller.list(authentication, request);

    assertEquals(List.of(dto), out);
    verify(service).listMyReservations(99L);
    verify(authUserService, never()).findOrCreateByEmail(any(), any());
  }

  @Test
  @DisplayName("create falls back to OAuth2 principal, stores session, returns 201 body")
  void create_fallbackToPrincipal() {
    when(request.getSession(false)).thenReturn(null);
    when(authentication.getPrincipal()).thenReturn(principal);
    when(principal.getAttribute("email")).thenReturn("u@test.com");
    when(principal.getAttribute("name")).thenReturn("User");
    when(authUserService.findOrCreateByEmail("u@test.com", "User")).thenReturn(77L);
    when(request.getSession(true)).thenReturn(session);

    final ReservationDetailResponse detail = new ReservationDetailResponse();
    when(service.createReservation(77L, null)).thenReturn(detail);

    final ResponseEntity<ReservationDetailResponse> resp =
        controller.create(authentication, request, null);

    assertEquals(201, resp.getStatusCodeValue());
    assertSame(detail, resp.getBody());
    verify(session).setAttribute(any(), any());
  }

  @Test
  @DisplayName("get delegates with session user id")
  void get_delegates() {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(any())).thenReturn(11L);
    final ReservationDetailResponse detail = new ReservationDetailResponse();
    when(service.getMyReservation(11L, 5L)).thenReturn(detail);

    final ReservationDetailResponse out =
        controller.get(authentication, request, 5L);

    assertSame(detail, out);
    verify(service).getMyReservation(11L, 5L);
  }

  @Test
  @DisplayName("patch delegates with resolved user id and payload")
  void patch_delegates() {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(any())).thenReturn(22L);
    final ReservationDetailResponse detail = new ReservationDetailResponse();
    final PatchReservationRequest req = new PatchReservationRequest();
    when(service.patchMyReservation(22L, 7L, req)).thenReturn(detail);

    final ReservationDetailResponse out =
        controller.patch(authentication, request, 7L, req);

    assertSame(detail, out);
    verify(service).patchMyReservation(22L, 7L, req);
  }

  @Test
  @DisplayName("cancel returns 204 and calls service.cancel")
  void cancel_noContent() {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(any())).thenReturn(33L);

    final ResponseEntity<Void> resp = controller.cancel(authentication, request, 9L);

    assertEquals(204, resp.getStatusCodeValue());
    assertTrue(resp.getHeaders().isEmpty());
    verify(service).cancelMyReservation(33L, 9L);
  }
}
