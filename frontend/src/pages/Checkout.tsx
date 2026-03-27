import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Checkout() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');
  const [couponCode, setCouponCode] = useState('');
  const [couponMsg, setCouponMsg] = useState('');
  const [couponValid, setCouponValid] = useState(false);
  const [cardNumber, setCardNumber] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvv, setCvv] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // Pre-fill email from auth cookie if logged in
  useEffect(() => {
    const cookie = document.cookie.split('; ').find(r => r.startsWith('auth_email='));
    if (cookie) setEmail(decodeURIComponent(cookie.split('=')[1]));
  }, []);

  async function applyCoupon() {
    setCouponMsg('');
    setCouponValid(false);
    if (!couponCode.trim()) return;
    const res = await fetch('/api/coupons/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: couponCode.trim() }),
    });
    const data = await res.json();
    if (res.ok && data.valid) {
      setCouponValid(true);
      setCouponMsg(`✓ ${data.description}`);
    } else {
      setCouponMsg(data.description || 'Invalid coupon code');
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError('');

    try {
      const res = await fetch('/api/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email,
          shippingAddress: address,
          couponCode: couponValid ? couponCode.trim().toUpperCase() : undefined,
          cardNumber,
          expiry,
          cvv,
        }),
      });

      const data = await res.json();

      if (!res.ok) {
        setError(data.error || 'Checkout failed. Please try again.');
        return;
      }

      const params = new URLSearchParams({
        txn:      data.transactionId,
        total:    data.total,
        subtotal: data.subtotal,
        discount: data.discountAmount ?? '0',
        coupon:   data.discountCode ?? '',
        taxState: data.taxState ?? '',
        taxRate:  String(data.taxRate ?? 0),
        taxAmt:   data.taxAmount ?? '0',
      });
      navigate(`/order/${data.orderId}?${params.toString()}`);
    } catch {
      setError('Could not reach the checkout service. Make sure all services are running.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-xl font-semibold mb-6">Checkout</h1>
      <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={e => setEmail(e.target.value)}
            placeholder="you@example.com"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Shipping Address</label>
          <textarea
            required
            value={address}
            onChange={e => setAddress(e.target.value)}
            placeholder="123 Main St, City, CA 90210"
            rows={3}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-400 resize-none"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Promo Code</label>
          <div className="flex gap-2">
            <input
              type="text"
              value={couponCode}
              onChange={e => { setCouponCode(e.target.value.toUpperCase()); setCouponMsg(''); setCouponValid(false); }}
              placeholder="e.g. DEMO10"
              className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-400"
            />
            <button
              type="button"
              onClick={applyCoupon}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition-colors"
            >
              Apply
            </button>
          </div>
          {couponMsg && (
            <p className={`text-xs mt-1 ${couponValid ? 'text-green-600' : 'text-red-500'}`}>{couponMsg}</p>
          )}
        </div>
        <div>
          <p className="text-sm font-medium mb-3">Payment Details</p>
          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium mb-1">Card Number</label>
              <input
                type="text"
                required
                value={cardNumber}
                onChange={e => setCardNumber(e.target.value)}
                placeholder="1234 5678 9012 3456"
                maxLength={19}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-400"
              />
            </div>
            <div className="flex gap-3">
              <div className="flex-1">
                <label className="block text-sm font-medium mb-1">Expiry (MM/YY)</label>
                <input
                  type="text"
                  required
                  value={expiry}
                  onChange={e => setExpiry(e.target.value)}
                  placeholder="MM/YY"
                  maxLength={5}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-400"
                />
              </div>
              <div className="flex-1">
                <label className="block text-sm font-medium mb-1">CVV</label>
                <input
                  type="text"
                  required
                  value={cvv}
                  onChange={e => setCvv(e.target.value)}
                  placeholder="123"
                  maxLength={4}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-400"
                />
              </div>
            </div>
          </div>
        </div>
        {error && <p className="text-red-600 text-sm">{error}</p>}
        <button
          type="submit"
          disabled={submitting}
          className="w-full bg-gray-900 text-white py-2.5 rounded-lg text-sm font-medium hover:bg-gray-700 transition-colors disabled:opacity-50"
        >
          {submitting ? 'Placing Order...' : 'Place Order'}
        </button>
      </form>
    </div>
  );
}
