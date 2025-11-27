import PropTypes from 'prop-types';
import './HotelCard.css';

const formatAddress = (hotel) => {
  const parts = [
    hotel.addressLine1,
    hotel.addressLine2,
    hotel.city,
    hotel.state,
    hotel.country,
    hotel.postalCode
  ].filter(Boolean);
  return parts.join(', ');
};

function HotelCard({ hotel, onReserve }) {
  return (
    <article className="hotel-card">
      <div className="card-header">
        <div>
          <p className="brand">{hotel.brand ?? 'Independent'}</p>
          <h3>{hotel.name}</h3>
        </div>
        {hotel.starRating && (
          <span className="rating">{hotel.starRating.toFixed(1)} &#9733;</span>
        )}
      </div>

      <p className="address">{formatAddress(hotel)}</p>

      <div className="meta">
        <div>
          <p className="label">City</p>
          <p className="value">{hotel.city ?? 'TBD'}</p>
        </div>
        <div>
          <p className="label">Country</p>
          <p className="value">{hotel.country ?? 'TBD'}</p>
        </div>
      </div>

      <div className="card-footer">
        <button type="button" className="book-button" onClick={onReserve}>
          Reserve Now
        </button>
      </div>
    </article>
  );
}

HotelCard.propTypes = {
  hotel: PropTypes.shape({
    id: PropTypes.number,
    name: PropTypes.string,
    brand: PropTypes.string,
    addressLine1: PropTypes.string,
    addressLine2: PropTypes.string,
    city: PropTypes.string,
    state: PropTypes.string,
    country: PropTypes.string,
    postalCode: PropTypes.string,
    starRating: PropTypes.number,
    createdAt: PropTypes.string
  }).isRequired,
  onReserve: PropTypes.func
};

HotelCard.defaultProps = {
  onReserve: undefined
};

export default HotelCard;
