import PropTypes from 'prop-types';
import './ReservationCard.css';

const formatDate = (value) =>
  value ? new Date(`${value}T00:00:00`).toLocaleDateString() : '--';

function ReservationCard({ reservation, onModify, onCancel }) {
  return (
    <article className="reservation-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">
            Status:{' '}
            <span
              className={`status status-${reservation.status?.toLowerCase()}`}
            >
              {reservation.status}
            </span>
          </p>
          <h3>{reservation.hotelName ?? 'Unnamed Hotel'}</h3>
          <p className="subheading">
            {reservation.roomTypeName ?? 'Room type to be assigned'}
          </p>
        </div>
      </div>

      <div className="reservation-meta">
        <div>
          <p className="label">Check-in</p>
          <p className="value">{formatDate(reservation.checkInDate)}</p>
        </div>
        <div>
          <p className="label">Check-out</p>
          <p className="value">{formatDate(reservation.checkOutDate)}</p>
        </div>
      </div>

      <div className="reservation-actions">
        <button type="button" className="secondary" onClick={onModify}>
          Modify
        </button>
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </article>
  );
}

ReservationCard.propTypes = {
  reservation: PropTypes.shape({
    id: PropTypes.number.isRequired,
    status: PropTypes.string,
    hotelName: PropTypes.string,
    roomTypeName: PropTypes.string,
    checkInDate: PropTypes.string,
    checkOutDate: PropTypes.string
  }).isRequired,
  onModify: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired
};

export default ReservationCard;
