import { useEffect, useMemo, useState } from 'react';
import SearchForm from './components/SearchForm.jsx';
import HotelCard from './components/HotelCard.jsx';
import './App.css';

const RAW_API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';
const API_BASE_URL = RAW_API_BASE.replace(/\/+$/, '');
const SESSION_COOKIE = import.meta.env.VITE_SESSION_COOKIE ?? '';

function App() {
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchCity, setSearchCity] = useState('');
  const [checkInDate, setCheckInDate] = useState('');
  const [checkOutDate, setCheckOutDate] = useState('');
  const [hasFetched, setHasFetched] = useState(false);

  useEffect(() => {
    if (SESSION_COOKIE) {
      document.cookie = `${SESSION_COOKIE}; path=/`;
    }
  }, []);

  const fetchHotels = async () => {
    setLoading(true);
    setError('');
    try {
      const hotelsUrl = API_BASE_URL ? `${API_BASE_URL}/hotels` : '/hotels';
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

  return (
    <div className="app-shell">
      <header className="hero">
        <div>
          {/* <p className="eyebrow">AirHotel Service Explorer</p> */}
          <h1>Find your next stay with confidence</h1>
          <p className="subtitle">
            Browse the hotels and reserve one with a signle click.
          </p>
        </div>
      </header>

      <main>
        <SearchForm
          searchCity={searchCity}
          onSearchCityChange={setSearchCity}
          checkInDate={checkInDate}
          checkOutDate={checkOutDate}
          onCheckInChange={setCheckInDate}
          onCheckOutChange={setCheckOutDate}
          onSearch={fetchHotels}
          loading={loading}
        />

        {error && <div className="alert alert-error">{error}</div>}
        {!error && !loading && hasFetched && filteredHotels.length === 0 && (
          <div className="alert alert-empty">
            No hotels matched{' '}
            {searchCity ? `"${searchCity}"` : 'your filters'}. Try another city
            or adjust the travel dates.
          </div>
        )}

        <section className="results">
          {loading && <div className="loading">Loading hotel data...</div>}
          {!loading &&
            filteredHotels.map((hotel) => (
              <HotelCard key={hotel.id} hotel={hotel} />
            ))}
        </section>
      </main>
    </div>
  );
}

export default App;
