import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import './Modal.css';

function RoomTypeModal({
  hotel,
  checkInDate,
  checkOutDate,
  onClose,
  fetchAvailability,
  onSubmitReservation,
  submitting
}) {
  const [numGuests, setNumGuests] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [roomTypes, setRoomTypes] = useState([]);
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState(null);
  const [page, setPage] = useState(1);
  const pageSize = 4;

  const load = async (guests) => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchAvailability(guests);
      const sorted = [...data].sort((a, b) => {
        const ax = a.baseRate ?? Number.MAX_SAFE_INTEGER;
        const bx = b.baseRate ?? Number.MAX_SAFE_INTEGER;
        return ax - bx;
      });
      setRoomTypes(sorted);
      setSelectedRoomTypeId(sorted[0]?.roomTypeId ?? null);
      setPage(1);
    } catch (err) {
      console.error('Failed to load room types', err);
      setError(
        err instanceof Error ? err.message : 'Failed to load room types.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(numGuests);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleRefresh = () => {
    if (numGuests <= 0) {
      setError('Guests must be at least 1');
      return;
    }
    load(numGuests);
  };

  const totalPages = Math.max(1, Math.ceil(roomTypes.length / pageSize));
  const currentPageItems = roomTypes.slice(
    (page - 1) * pageSize,
    page * pageSize
  );
  const selectedRoomType = roomTypes.find(
    (rt) => rt.roomTypeId === selectedRoomTypeId
  );

  return (
    <div className="modal-backdrop">
      <div className="modal large">
        <div className="modal-header">
          <div>
            <p className="eyebrow">Room types</p>
            <h3>{hotel?.name ?? 'Selected Hotel'}</h3>
            <p className="subheading">
              {checkInDate} to {checkOutDate} · sorted by price
            </p>
          </div>
          <button type="button" className="close-button" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="modal-body compact">
          <div className="inline-fields">
            <label>
              Guests
              <input
                type="number"
                min="1"
                value={numGuests}
                onChange={(e) => setNumGuests(Number(e.target.value))}
              />
            </label>
            <button
              type="button"
              className="refresh-btn"
              onClick={handleRefresh}
              disabled={loading}
            >
              {loading ? 'Loading...' : 'Refresh availability'}
            </button>
          </div>

          {error && <div className="modal-error">{error}</div>}

          <div className="roomtype-list compact">
            {loading && <div className="loading">Loading availability...</div>}

            {!loading && currentPageItems.length === 0 && (
              <div className="alert alert-empty">
                No room types available for the selected dates.
              </div>
            )}

            {!loading &&
              currentPageItems.map((rt) => {
                const isSelected = selectedRoomTypeId === rt.roomTypeId;
                return (
                  <button
                    type="button"
                    key={rt.roomTypeId}
                    className={`roomtype-card${isSelected ? ' selected' : ''}`}
                    onClick={() => setSelectedRoomTypeId(rt.roomTypeId)}
                    aria-pressed={isSelected}
                  >
                    <div className="roomtype-card-content">
                      <div className="roomtype-card-header">
                        <div>
                          <p className="roomtype-name">{rt.name}</p>
                          <p className="roomtype-code">
                            Code: {rt.code ?? 'TBD'}
                          </p>
                        </div>
                        <div className="price-tag">
                          {rt.baseRate ? `$${rt.baseRate}` : 'Rate TBD'}
                        </div>
                      </div>

                      <div className="chip-row">
                        <span className="chip">
                          Bed: {rt.bedType ?? 'TBD'}
                        </span>
                        <span className="chip">
                          Capacity: {rt.capacity ?? '--'} guests
                        </span>
                        <span
                          className={`chip ${
                            rt.available > 0 ? 'chip-success' : 'chip-muted'
                          }`}
                        >
                          {rt.available} of {rt.totalRooms ?? '--'} available
                        </span>
                      </div>
                    </div>
                  </button>
                );
              })}
          </div>

          {roomTypes.length > pageSize && (
            <div className="pagination dense">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
              >
                Prev
              </button>
              <div>
                Page {page} of {totalPages}
              </div>
              <button
                type="button"
                onClick={() =>
                  setPage((p) => Math.min(totalPages, p + 1))
                }
                disabled={page >= totalPages}
              >
                Next
              </button>
            </div>
          )}

          <div className="modal-actions tight">
            <button type="button" className="secondary" onClick={onClose}>
              Close
            </button>
            <button
              type="button"
              disabled={!selectedRoomTypeId || submitting}
              onClick={() =>
                onSubmitReservation({
                  roomTypeId: selectedRoomTypeId,
                  numGuests,
                  baseRate: selectedRoomType?.baseRate ?? 0
                })
              }
            >
              {submitting ? 'Submitting...' : 'Submit'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

RoomTypeModal.propTypes = {
  hotel: PropTypes.shape({
    id: PropTypes.number,
    name: PropTypes.string
  }).isRequired,
  checkInDate: PropTypes.string.isRequired,
  checkOutDate: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  fetchAvailability: PropTypes.func.isRequired,
  onSubmitReservation: PropTypes.func.isRequired,
  submitting: PropTypes.bool
};

RoomTypeModal.defaultProps = {
  submitting: false
};

export default RoomTypeModal;
