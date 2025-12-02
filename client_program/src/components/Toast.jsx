import PropTypes from 'prop-types';
import './Toast.css';

function Toast({ toast, onClose }) {
  return (
    <div className={`toast ${toast.type === 'error' ? 'toast-error' : 'toast-success'}`}>
      <span>{toast.message}</span>
      <button type="button" onClick={onClose} aria-label="Close">
        ×
      </button>
    </div>
  );
}

Toast.propTypes = {
  toast: PropTypes.shape({
    id: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    type: PropTypes.oneOf(['success', 'error'])
  }).isRequired,
  onClose: PropTypes.func.isRequired
};

export default Toast;
