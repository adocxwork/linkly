import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts';
import api, { errorMessage } from '../api';

const COLORS = ['#0071e3', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

const AnalyticsModal = ({ link, onClose }) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        const res = await api.get(`/links/${link.id}/analytics`);
        setData(res.data);
      } catch (err) {
        setError(errorMessage(err, 'Failed to load analytics'));
      } finally {
        setLoading(false);
      }
    };
    fetchAnalytics();
  }, [link.id]);

  const formatData = (obj) => {
    return Object.keys(obj || {}).map(key => ({
      name: key,
      value: obj[key]
    })).filter(item => item.value > 0);
  };

  return (
    <AnimatePresence>
      <div style={{
        position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
        backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1000,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '1rem'
      }}>
        <motion.div 
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.9 }}
          className="card glass"
          style={{ width: '100%', maxWidth: '800px', maxHeight: '90vh', overflowY: 'auto' }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem' }}>
            <h2>Analytics: {link.title}</h2>
            <button onClick={onClose} className="btn btn-secondary">Close</button>
          </div>

          {loading && <p className="text-center text-secondary">Loading analytics...</p>}
          {!loading && error && <p className="text-center text-danger">{error}</p>}
          {!loading && data && (
            <>
            <div className="flex gap-4 mb-6" style={{ flexWrap: 'wrap' }}>
              <div className="card" style={{ flex: '1 1 140px', padding: '1rem' }}>
                <div className="text-secondary" style={{ fontSize: '0.85rem' }}>Total clicks</div>
                <h2 style={{ margin: 0 }}>{data.totalClicks}</h2>
              </div>
              <div className="card" style={{ flex: '1 1 140px', padding: '1rem' }}>
                <div className="text-secondary" style={{ fontSize: '0.85rem' }}>Unique visitors</div>
                <h2 style={{ margin: 0 }}>{data.uniqueVisitors}</h2>
              </div>
            </div>
            <h4 className="mb-4">Clicks over the last 30 days</h4>
            <div style={{ height: '200px', marginBottom: '2rem' }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={(data.clicksOverTime || []).map(d => ({ day: d.date.slice(5), clicks: d.clicks }))}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-color)" />
                  <XAxis dataKey="day" tick={{ fontSize: 11 }} interval={4} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} width={30} />
                  <Tooltip />
                  <Bar dataKey="clicks" fill="#0071e3" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '2rem' }}>
              
              <div>
                <h4 className="text-center mb-4">By Country</h4>
                <div style={{ height: '200px' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={formatData(data.clicksByCountry)} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                        {formatData(data.clicksByCountry).map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div>
                <h4 className="text-center mb-4">By Device</h4>
                <div style={{ height: '200px' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={formatData(data.clicksByDevice)} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                        {formatData(data.clicksByDevice).map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div>
                <h4 className="text-center mb-4">By Browser</h4>
                <div style={{ height: '200px' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={formatData(data.clicksByBrowser)} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                        {formatData(data.clicksByBrowser).map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

            </div>
            </>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  );
};

export default AnalyticsModal;
