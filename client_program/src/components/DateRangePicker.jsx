import { useEffect, useMemo, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { DateRange } from 'react-date-range';
import { addDays, parseISO, isAfter } from 'date-fns';
import './DateRangePicker.css';
import 'react-date-range/dist/styles.css';
import 'react-date-range/dist/theme/default.css';

const toDate = (value) => (value ? parseISO(value) : null);
const toIso = (date) => (date ? date.toISOString().split('T')[0] : '');

function DateRangePicker({
  label,
  checkInDate,
  checkOutDate,
  minCheckInDate,
  onChange
}) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  const minStart = useMemo(
    () => toDate(minCheckInDate) ?? addDays(new Date(), 1),
    [minCheckInDate]
  );

  const selectionRange = useMemo(() => {
    const start = toDate(checkInDate) ?? minStart;
    const endCandidate = toDate(checkOutDate);
    const end =
      endCandidate && isAfter(endCandidate, start)
        ? endCandidate
        : addDays(start, 1);
    return [
      {
        startDate: start,
        endDate: end,
        key: 'selection'
      }
    ];
  }, [checkInDate, checkOutDate, minStart]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    if (open) {
      document.addEventListener('mousedown', handleClickOutside);
    } else {
      document.removeEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  const handleSelect = (ranges) => {
    const { startDate, endDate } = ranges.selection;
    onChange(toIso(startDate), toIso(endDate));
  };

  const summary =
    checkInDate && checkOutDate
      ? `${checkInDate} → ${checkOutDate}`
      : 'Select dates';

  return (
    <div className="drp-wrapper" ref={containerRef}>
      {label && <label className="drp-label">{label}</label>}
      <button
        type="button"
        className="drp-trigger"
        onClick={() => setOpen((v) => !v)}
      >
        <span>{summary}</span>
        <span className="drp-icon">📅</span>
      </button>

      {open && (
        <div className="drp-popover">
          <DateRange
            ranges={selectionRange}
            minDate={minStart}
            onChange={handleSelect}
            moveRangeOnFirstSelection={false}
            showDateDisplay={false}
            rangeColors={['#0ea5e9']}
          />
          <div className="drp-actions">
            <button type="button" onClick={() => setOpen(false)}>
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

DateRangePicker.propTypes = {
  label: PropTypes.string,
  checkInDate: PropTypes.string,
  checkOutDate: PropTypes.string,
  minCheckInDate: PropTypes.string,
  onChange: PropTypes.func.isRequired
};

DateRangePicker.defaultProps = {
  label: 'Dates',
  checkInDate: '',
  checkOutDate: '',
  minCheckInDate: ''
};

export default DateRangePicker;
