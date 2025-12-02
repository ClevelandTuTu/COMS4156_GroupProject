import PropTypes from 'prop-types';
import DateRangePicker from './DateRangePicker.jsx';
import './SearchForm.css';

function SearchForm({
  searchCity,
  onSearchCityChange,
  checkInDate,
  checkOutDate,
  onCheckInChange,
  onCheckOutChange,
  onSearch,
  loading,
  minCheckInDate,
  minCheckOutDate
}) {
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

      <div className="field date-range-field">
        <DateRangePicker
          label="Dates"
          checkInDate={checkInDate}
          checkOutDate={checkOutDate}
          minCheckInDate={minCheckInDate}
          onChange={(start, end) => {
            onCheckInChange(start);
            onCheckOutChange(end);
          }}
        />
      </div>

      <button
        type="button"
        className="search-button"
        onClick={onSearch}
        disabled={loading}
      >
        {loading ? 'Loading...' : 'Show All Hotels'}
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
  loading: PropTypes.bool,
  minCheckInDate: PropTypes.string,
  minCheckOutDate: PropTypes.string
};

SearchForm.defaultProps = {
  loading: false,
  minCheckInDate: undefined,
  minCheckOutDate: undefined
};

export default SearchForm;
