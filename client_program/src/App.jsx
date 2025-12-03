import { useEffect, useMemo, useState } from 'react';
import SearchForm from './components/SearchForm.jsx';
import HotelCard from './components/HotelCard.jsx';
import ReservationCard from './components/ReservationCard.jsx';
import ReservationEditModal from './components/ReservationEditModal.jsx';
import ConfirmModal from './components/ConfirmModal.jsx';
import Toast from './components/Toast.jsx';
import RoomTypeModal from './components/RoomTypeModal.jsx';
import './App.css';

const RAW_API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';
const API_BASE_URL = RAW_API_BASE.replace(/\/+$/, '');

const buildApiUrl = (path) =>
  API_BASE_URL ? `${API_BASE_URL}${path}` : path;

function App() {
  const hasJSessionCookie = () =>
    document.cookie.split(';').some((c) => c.trim().startsWith('JSESSIONID='));

  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchCity, setSearchCity] = useState('');
  const [checkInDate, setCheckInDate] = useState('');
  const [checkOutDate, setCheckOutDate] = useState('');
  const [hasFetched, setHasFetched] = useState(false);
  const [activeView, setActiveView] = useState('search');
  const [reservations, setReservations] = useState([]);
  const [reservationsLoading, setReservationsLoading] = useState(false);
  const [reservationsError, setReservationsError] = useState('');
  const [reservationsFetched, setReservationsFetched] = useState(false);
  const [selectedReservation, setSelectedReservation] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [creatingReservation, setCreatingReservation] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [showRoomTypeModal, setShowRoomTypeModal] = useState(false);
  const [selectedHotel, setSelectedHotel] = useState(null);
  const [hasSession, setHasSession] = useState(false);
  const isModalOpen = showEditModal || showCancelModal || showRoomTypeModal;

  const today = new Date();
  const tomorrowIso = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate() + 1
  )
    .toISOString()
    .split('T')[0];

  const parseLocalDate = (value) =>
    value ? new Date(`${value}T00:00:00`) : null;

  const persistSessionFromPayload = (payload) => {
    const sessionId = payload?.session?.jsessionId;
    if (sessionId) {
      document.cookie = `JSESSIONID=${sessionId}; path=/`;
      setHasSession(true);
    }
  };

  useEffect(() => {
    if (hasJSessionCookie()) {
      setHasSession(true);
    }
    // Silent session probe in case cookie is HttpOnly and unreadable
    const probeSession = async () => {
      try {
        const resp = await fetch(buildApiUrl('/hotels'), {
          credentials: 'include',
          mode: API_BASE_URL ? 'cors' : 'same-origin'
        });
        if (resp.ok) {
          setHasSession(true);
        }
      } catch (e) {
        // ignore probe errors
      }
    };
    if (!hasSession) {
      probeSession();
    }
  }, []);

  const handleLogin = () => {
    window.location.href = buildApiUrl('/oauth2/authorization/google');
  };

  const addDays = (dateStr, days) => {
    const d = parseLocalDate(dateStr);
    if (!d) return '';
    d.setDate(d.getDate() + days);
    return d.toISOString().split('T')[0];
  };

  const calculateNights = (start, end) => {
    if (!start || !end) return 0;
    const startDate = parseLocalDate(start);
    const endDate = parseLocalDate(end);
    if (!startDate || !endDate) return 0;
    const diffMs = endDate.getTime() - startDate.getTime();
    const nights = Math.round(diffMs / (1000 * 60 * 60 * 24));
    return nights > 0 ? nights : 0;
  };

  const logout = async () => {
    try {
      const url = buildApiUrl('/logout');
      const resp = await fetch(url, {
        credentials: 'include',
        mode: API_BASE_URL ? 'cors' : 'same-origin'
      });
      if (!resp.ok) {
        throw new Error(`Logout failed with status ${resp.status}`);
      }
      setHasSession(false);
      setHotels([]);
      setHasFetched(false);
      setReservations([]);
      setReservationsFetched(false);
      setSearchCity('');
      setCheckInDate('');
      setCheckOutDate('');
      addToast('Logged out.', 'success');
      document.cookie = 'JSESSIONID=; Max-Age=0; path=/';
    } catch (err) {
      console.error('Logout failed', err);
      addToast(
        err instanceof Error ? err.message : 'Logout failed.',
        'error'
      );
    }
  };

  useEffect(() => {
    if (isModalOpen) {
      document.body.classList.add('modal-open');
    } else {
      document.body.classList.remove('modal-open');
    }
    return () => document.body.classList.remove('modal-open');
  }, [isModalOpen]);

  const fetchHotels = async () => {
    if (!hasSession) {
      setError('');
      setHasFetched(false);
      return;
    }
    if (!searchCity.trim()) {
      addToast('Please enter a destination.', 'error');
      return;
    }
    if (!checkInDate || !checkOutDate) {
      addToast('Please select check-in and check-out dates.', 'error');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (searchCity) {
        params.append('city', searchCity);
      }
      params.append('startDate', checkInDate);
      params.append('endDate', checkOutDate);
      const hotelsUrl = buildApiUrl(`/hotels/search/available?${params.toString()}`);
      const response = await fetch(hotelsUrl, {
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        mode: API_BASE_URL ? 'cors' : 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const data = await response.json();
      persistSessionFromPayload(data);
      setHasSession(true);
      setHotels(Array.isArray(data) ? data : []);
      setHasFetched(true);
    } catch (err) {
      console.error('Failed to load hotels', err);
      setError(
        err instanceof Error
          ? err.message
          : 'Unable to load hotels. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  const fetchReservations = async () => {
    if (!hasSession) {
      setReservationsError('');
      setReservationsFetched(false);
      return;
    }
    setReservationsLoading(true);
    setReservationsError('');
    try {
      const reservationsUrl = buildApiUrl('/reservations');
      const response = await fetch(reservationsUrl, {
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        mode: API_BASE_URL ? 'cors' : 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const data = await response.json();
      persistSessionFromPayload(data);
      setHasSession(true);
      setReservations(Array.isArray(data) ? data : []);
      setReservationsFetched(true);
    } catch (err) {
      console.error('Failed to load reservations', err);
      setReservationsError(
        err instanceof Error
          ? err.message
          : 'Unable to load reservations. Please try again.'
      );
    } finally {
      setReservationsLoading(false);
    }
  };

  useEffect(() => {
    if (activeView === 'reservations' && !reservationsFetched && !reservationsLoading) {
      fetchReservations();
    }
  }, [activeView, reservationsFetched, reservationsLoading]);

  const filteredHotels = useMemo(() => {
    if (!searchCity.trim()) {
      return hotels;
    }
    return hotels.filter((hotel) =>
      `${hotel.city ?? ''} ${hotel.name ?? ''}`
        .toLowerCase()
        .includes(searchCity.trim().toLowerCase())
    );
  }, [hotels, searchCity]);

  const groupedReservations = useMemo(() => {
    const toTime = (value, fallback = Number.MAX_SAFE_INTEGER) => {
      const d = parseLocalDate(value);
      const time = d ? d.getTime() : fallback;
      return Number.isNaN(time) ? fallback : time;
    };
    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);
    const todayMs = startOfToday.getTime();

    const sorted = [...reservations].sort(
      (a, b) => toTime(a.checkInDate) - toTime(b.checkInDate)
    );
    const upcoming = [];
    const past = [];
    const canceled = [];
    sorted.forEach((r) => {
      const status = (r.status ?? '').toUpperCase();
      if (status === 'CANCELED') {
        canceled.push(r);
        return;
      }
      const checkOutMs = toTime(r.checkOutDate, 0);
      // Past only after the stay has completed; ongoing stays remain upcoming.
      if (checkOutMs < todayMs) {
        past.push(r);
        return;
      }
      upcoming.push(r);
    });
    return { upcoming, past, canceled };
  }, [reservations]);

  const onNavChange = (view) => {
    setActiveView(view);
  };

  const handleCheckInChange = (value) => {
    setCheckInDate(value);
    if (value) {
      const minOut = addDays(value, 1);
      if (checkOutDate && checkOutDate < minOut) {
        setCheckOutDate('');
      }
    } else {
      setCheckOutDate('');
    }
  };

  const openEditModal = (reservation) => {
    setSelectedReservation(reservation);
    setSubmitError('');
    setShowEditModal(true);
  };

  const openCancelModal = (reservation) => {
    setSelectedReservation(reservation);
    setSubmitError('');
    setShowCancelModal(true);
  };

  const openRoomTypeModal = (hotel) => {
    if (!checkInDate || !checkOutDate) {
      addToast('Please select check-in and check-out before reserving.', 'error');
      return;
    }
    setSelectedHotel(hotel);
    setShowRoomTypeModal(true);
  };

  const closeModals = () => {
    setShowEditModal(false);
    setShowCancelModal(false);
    setShowRoomTypeModal(false);
    setSelectedReservation(null);
    setSelectedHotel(null);
    setSubmitting(false);
    setCreatingReservation(false);
    setSubmitError('');
  };

  const handleUpdateReservation = async (payload) => {
    if (!selectedReservation) {
      return;
    }
    setSubmitting(true);
    setSubmitError('');
    try {
      const nights = calculateNights(
        payload.checkInDate,
        payload.checkOutDate
      );
      const url = buildApiUrl(`/reservations/${selectedReservation.id}`);
      const response = await fetch(url, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        mode: API_BASE_URL ? 'cors' : 'same-origin',
        body: JSON.stringify({
          checkInDate: payload.checkInDate,
          checkOutDate: payload.checkOutDate,
          nights
        })
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed with status ${response.status}`);
      }
      const data = await response.json();
      persistSessionFromPayload(data);
      addToast('Reservation updated successfully.', 'success');
      closeModals();
      fetchReservations();
    } catch (err) {
      console.error('Failed to update reservation', err);
      setSubmitError(
        err instanceof Error ? err.message : 'Failed to update reservation.'
      );
      addToast(
        err instanceof Error ? err.message : 'Failed to update reservation.',
        'error'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelReservation = async () => {
    if (!selectedReservation) {
      return;
    }
    setSubmitting(true);
    setSubmitError('');
    try {
      const url = buildApiUrl(`/reservations/${selectedReservation.id}`);
      const response = await fetch(url, {
        method: 'DELETE',
        credentials: 'include',
        mode: API_BASE_URL ? 'cors' : 'same-origin'
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed with status ${response.status}`);
      }
      addToast('Reservation canceled.', 'success');
      closeModals();
      fetchReservations();
    } catch (err) {
      console.error('Failed to cancel reservation', err);
      setSubmitError(
        err instanceof Error ? err.message : 'Failed to cancel reservation.'
      );
      addToast(
        err instanceof Error ? err.message : 'Failed to cancel reservation.',
        'error'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateReservation = async ({ roomTypeId, numGuests, baseRate }) => {
    if (!selectedHotel || !roomTypeId || !checkInDate || !checkOutDate) {
      addToast('Please select a room type and dates.', 'error');
      return;
    }
    const nights = calculateNights(checkInDate, checkOutDate);
    const priceTotal =
      typeof baseRate === 'number' && !Number.isNaN(baseRate)
        ? Math.max(0, baseRate * nights)
        : 0;

    setCreatingReservation(true);
    try {
      const url = buildApiUrl('/reservations');
      const payload = {
        hotelId: selectedHotel.id,
        roomTypeId,
        checkInDate,
        checkOutDate,
        nights,
        numGuests,
        currency: 'USD',
        priceTotal,
        notes: ''
      };
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        mode: API_BASE_URL ? 'cors' : 'same-origin',
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed with status ${response.status}`);
      }
      const created = await response.json();
      persistSessionFromPayload(created);
      addToast('Reservation created successfully.', 'success');
      closeModals();
      setActiveView('reservations');
      fetchReservations();
    } catch (err) {
      console.error('Failed to create reservation', err);
      addToast(
        err instanceof Error ? err.message : 'Failed to create reservation.',
        'error'
      );
    } finally {
      setCreatingReservation(false);
    }
  };

  const addToast = (message, type = 'success') => {
    const id =
      (typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random()}`);
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3500);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  return (
    <div className="app-shell">
      <header className="hero">
        <div className="hero-top">
          <div>
            <h1>Plan your stay in minutes</h1>
            <p className="subtitle">
              Browse hotels and reserve with a single click.
            </p>
          </div>
          <div className="auth-actions">
            {hasSession ? (
              <button
                type="button"
                className="secondary"
                onClick={logout}
              >
                Log out
              </button>
            ) : (
              <button
                type="button"
                className="primary"
                onClick={handleLogin}
              >
                Sign in
              </button>
            )}
          </div>
        </div>
      </header>

      <nav className="nav-bar">
        <button
          type="button"
          className={activeView === 'search' ? 'nav-item active' : 'nav-item'}
          onClick={() => onNavChange('search')}
        >
          Search
        </button>
        <button
          type="button"
          className={
            activeView === 'reservations' ? 'nav-item active' : 'nav-item'
          }
          onClick={() => onNavChange('reservations')}
        >
          My Reservations
        </button>
      </nav>

      <main>
        {activeView === 'search' && (
          <>
            {!hasSession && (
              <div className="alert alert-empty">
                Please sign in to search and view hotels.
              </div>
            )}
            <SearchForm
              searchCity={searchCity}
              onSearchCityChange={setSearchCity}
              checkInDate={checkInDate}
              checkOutDate={checkOutDate}
              onCheckInChange={handleCheckInChange}
              onCheckOutChange={setCheckOutDate}
              onSearch={fetchHotels}
              loading={loading}
              minCheckInDate={tomorrowIso}
              minCheckOutDate={
                checkInDate ? addDays(checkInDate, 1) : undefined
              }
            />

            {hasSession && error && (
              <div className="alert alert-error">{error}</div>
            )}
            {!error && !loading && hasFetched && filteredHotels.length === 0 && (
              <div className="alert alert-empty">
                No hotels matched{' '}
                {searchCity ? `"${searchCity}"` : 'your filters'}. Try another
                city or adjust the travel dates.
              </div>
            )}

            <section className="results">
              {hasSession && loading && (
                <div className="loading">Loading hotel data...</div>
              )}
              {hasSession &&
                !loading &&
                filteredHotels.map((hotel) => (
                  <HotelCard
                    key={hotel.id}
                    hotel={hotel}
                    onReserve={() => openRoomTypeModal(hotel)}
                  />
                ))}
            </section>
          </>
        )}

        {activeView === 'reservations' && (
          <>
            {reservationsError && (
              <div className="alert alert-error">{reservationsError}</div>
            )}
            {hasSession && reservationsLoading && (
              <div className="loading">Loading reservations...</div>
            )}
            {!hasSession && (
              <div className="alert alert-empty">
                Please sign in to view reservations.
              </div>
            )}
            {hasSession && !reservationsLoading && reservations.length === 0 && (
              <div className="alert alert-empty">
                You have no reservations yet.
              </div>
            )}
            {hasSession && !reservationsLoading && reservations.length > 0 && (
              <>
                {groupedReservations.upcoming.length > 0 && (
                  <>
                    <h3 className="section-title">Upcoming</h3>
                    <section className="reservations-grid">
                      {groupedReservations.upcoming.map((reservation) => (
                        <ReservationCard
                          key={reservation.id}
                          reservation={reservation}
                          onModify={() => openEditModal(reservation)}
                          onCancel={() => openCancelModal(reservation)}
                        />
                      ))}
                    </section>
                  </>
                )}

                {groupedReservations.past.length > 0 && (
                  <>
                    <h3 className="section-title muted">Past</h3>
                    <section className="reservations-grid">
                      {groupedReservations.past.map((reservation) => (
                        <ReservationCard
                          key={reservation.id}
                          reservation={reservation}
                          onModify={() => openEditModal(reservation)}
                          onCancel={() => openCancelModal(reservation)}
                        />
                      ))}
                    </section>
                  </>
                )}

                {groupedReservations.canceled.length > 0 && (
                  <>
                    <h3 className="section-title muted">Canceled</h3>
                    <section className="reservations-grid">
                      {groupedReservations.canceled.map((reservation) => (
                        <ReservationCard
                          key={reservation.id}
                          reservation={reservation}
                          onModify={() => openEditModal(reservation)}
                          onCancel={() => openCancelModal(reservation)}
                        />
                      ))}
                    </section>
                  </>
                )}
              </>
            )}
          </>
        )}
      </main>

      {showEditModal && selectedReservation && (
        <ReservationEditModal
          reservation={selectedReservation}
          onClose={closeModals}
          onSubmit={handleUpdateReservation}
          submitting={submitting}
          error={submitError}
        />
      )}

      {showCancelModal && selectedReservation && (
        <ConfirmModal
          title="Cancel reservation?"
          message="Are you sure you want to cancel this reservation?"
          confirmLabel="Confirm Cancel"
          cancelLabel="Keep"
          onClose={closeModals}
          onConfirm={handleCancelReservation}
          submitting={submitting}
          error={submitError}
        />
      )}

      {showRoomTypeModal && selectedHotel && (
        <RoomTypeModal
          hotel={selectedHotel}
          checkInDate={checkInDate}
          checkOutDate={checkOutDate}
          onClose={closeModals}
          fetchAvailability={async (numGuests) => {
            const url = buildApiUrl(
              `/hotels/${selectedHotel.id}/room-types/availability?checkIn=${checkInDate}&checkOut=${checkOutDate}${
                numGuests ? `&numGuests=${numGuests}` : ''
              }`
            );
            const resp = await fetch(url, {
              credentials: 'include',
              mode: API_BASE_URL ? 'cors' : 'same-origin'
            });
            if (!resp.ok) {
              const txt = await resp.text();
              throw new Error(txt || `Request failed with status ${resp.status}`);
            }
            const data = await resp.json();
            persistSessionFromPayload(data);
            return data;
          }}
          onSubmitReservation={handleCreateReservation}
          submitting={creatingReservation}
        />
      )}

      <div className="toast-container">
        {toasts.map((t) => (
          <Toast key={t.id} toast={t} onClose={() => removeToast(t.id)} />
        ))}
      </div>
    </div>
  );
}

export default App;
