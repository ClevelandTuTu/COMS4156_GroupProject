import PropTypes from 'prop-types';
import './SearchForm.css';

function SearchForm({
  searchCity,
  onSearchCityChange,
  checkInDate,
  checkOutDate,
  onCheckInChange,
  onCheckOutChange,
  onSearch,
  loading
}) {
  const today = new Date().toISOString().split('T')[0];

  return (
    <section className="search-form">
      <div className="field">
        <label htmlFor="city-input">Destination</label>
        <input
          id="city-input"
          type="text"
          placeholder="City, hotel name, or landmark"
          value={searchCity}
          onChange={(event) => onSearchCityChange(event.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="check-in">Check-in</label>
        <input
          id="check-in"
          type="date"
          min={today}
          value={checkInDate}
          onChange={(event) => onCheckInChange(event.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="check-out">Check-out</label>
        <input
          id="check-out"
          type="date"
          min={checkInDate || today}
          value={checkOutDate}
          onChange={(event) => onCheckOutChange(event.target.value)}
        />
      </div>

      <button
        type="button"
        className="search-button"
        onClick={onSearch}
        disabled={loading}
      >
        {loading ? 'Loading…' : 'Show All Hotels'}
      </button>
    </section>
  );
}

SearchForm.propTypes = {
  searchCity: PropTypes.string.isRequired,
  onSearchCityChange: PropTypes.func.isRequired,
  checkInDate: PropTypes.string.isRequired,
  checkOutDate: PropTypes.string.isRequired,
  onCheckInChange: PropTypes.func.isRequired,
  onCheckOutChange: PropTypes.func.isRequired,
  onSearch: PropTypes.func.isRequired,
  loading: PropTypes.bool
};

SearchForm.defaultProps = {
  loading: false
};

export default SearchForm;
