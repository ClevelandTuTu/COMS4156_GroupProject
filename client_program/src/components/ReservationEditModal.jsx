import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import DateRangePicker from './DateRangePicker.jsx';
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
  const today = new Date().toISOString().split('T')[0];

  useEffect(() => {
    if (reservation) {
      setCheckInDate(reservation.checkInDate ?? '');
      setCheckOutDate(reservation.checkOutDate ?? '');
    }
  }, [reservation]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      checkInDate,
      checkOutDate
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
          <DateRangePicker
            label="Stay Dates"
            checkInDate={checkInDate}
            checkOutDate={checkOutDate}
            minCheckInDate={today}
            onChange={(start, end) => {
              setCheckInDate(start);
              setCheckOutDate(end);
            }}
          />
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
    checkOutDate: PropTypes.string
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
