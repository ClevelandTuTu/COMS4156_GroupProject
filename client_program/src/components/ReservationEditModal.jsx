import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import './Modal.css';

function ReservationEditModal({
  reservation,
  onClose,
  onSubmit,
  submitting,
  error
}) {
  const [checkInDate, setCheckInDate] = useState('');
  const [checkOutDate, setCheckOutDate] = useState('');
  const [numGuests, setNumGuests] = useState(1);
  const today = new Date().toISOString().split('T')[0];

  const addDays = (dateStr, days) => {
    const d = new Date(dateStr);
    d.setDate(d.getDate() + days);
    return d.toISOString().split('T')[0];
  };

  useEffect(() => {
    if (reservation) {
      setCheckInDate(reservation.checkInDate ?? '');
      setCheckOutDate(reservation.checkOutDate ?? '');
      setNumGuests(reservation.numGuests ?? 1);
    }
  }, [reservation]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      checkInDate,
      checkOutDate,
      numGuests
    });
  };

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <div className="modal-header">
          <h3>Modify Reservation</h3>
          <button type="button" className="close-button" onClick={onClose}>
            ×
          </button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body">
          <label>
            Check-in
            <input
              type="date"
              min={today}
              value={checkInDate}
              onChange={(e) => {
                const next = e.target.value;
                setCheckInDate(next);
                if (next) {
                  const minOut = addDays(next, 1);
                  if (checkOutDate && checkOutDate < minOut) {
                    setCheckOutDate('');
                  }
                } else {
                  setCheckOutDate('');
                }
              }}
              required
            />
          </label>
          <label>
            Check-out
            <input
              type="date"
              min={checkInDate ? addDays(checkInDate, 1) : undefined}
              value={checkOutDate}
              onChange={(e) => setCheckOutDate(e.target.value)}
              disabled={!checkInDate}
              required
            />
          </label>
          <label>
            Guests
            <input
              type="number"
              min="1"
              value={numGuests}
              onChange={(e) => setNumGuests(Number(e.target.value))}
              required
            />
          </label>
          {error && <div className="modal-error">{error}</div>}
          <div className="modal-actions">
            <button
              type="button"
              className="secondary"
              onClick={onClose}
              disabled={submitting}
            >
              Cancel
            </button>
            <button type="submit" disabled={submitting}>
              {submitting ? 'Saving...' : 'Submit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

ReservationEditModal.propTypes = {
  reservation: PropTypes.shape({
    id: PropTypes.number.isRequired,
    checkInDate: PropTypes.string,
    checkOutDate: PropTypes.string,
    numGuests: PropTypes.number
  }).isRequired,
  onClose: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
  submitting: PropTypes.bool,
  error: PropTypes.string
};

ReservationEditModal.defaultProps = {
  submitting: false,
  error: ''
};

export default ReservationEditModal;
