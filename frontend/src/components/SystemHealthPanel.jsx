import React, { useCallback, useEffect, useState } from 'react';
import { Activity, RefreshCw } from 'lucide-react';
import api, { errorMessage } from '../api';

const STATUS_COLORS = {
  UP: '#10b981',
  DEGRADED: '#f59e0b',
  DOWN: '#ef4444',
  DISABLED: '#9ca3af',
};

const LABELS = {
  database: 'PostgreSQL (Supabase)',
  redis: 'Redis (Upstash)',
  clickPipeline: 'Click analytics pipeline',
  geoIp: 'Geo-IP lookup (geojs.io)',
  keepAlive: 'Self-ping (keep-alive)',
  jvm: 'Backend JVM',
};

const Badge = ({ status }) => (
  <span style={{
    fontSize: '0.75rem', fontWeight: 600, color: 'white', padding: '2px 8px', borderRadius: '999px',
    background: STATUS_COLORS[status] || '#9ca3af',
  }}>{status}</span>
);

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'yes' : 'no';
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) return new Date(value).toLocaleString();
  return String(value);
};

const Toggle = ({ checked, onChange, disabled }) => (
  <button
    onClick={onChange}
    disabled={disabled}
    aria-pressed={checked}
    style={{
      width: '44px', height: '24px', borderRadius: '12px', border: 'none', cursor: disabled ? 'wait' : 'pointer',
      background: checked ? '#10b981' : '#d1d5db', position: 'relative', transition: 'background 0.3s', flexShrink: 0,
    }}
  >
    <div style={{
      width: '20px', height: '20px', borderRadius: '50%', background: 'white', position: 'absolute', top: '2px',
      left: checked ? '22px' : '2px', transition: 'left 0.3s', boxShadow: '0 2px 4px rgba(0,0,0,0.15)',
    }} />
  </button>
);

const SystemHealthPanel = ({ onError }) => {
  const [health, setHealth] = useState(null);
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [expanded, setExpanded] = useState({});

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [h, s] = await Promise.all([api.get('/admin/system/health'), api.get('/admin/system/settings')]);
      setHealth(h.data);
      setSettings(s.data);
    } catch (err) {
      onError(errorMessage(err, 'Could not load system health'));
    } finally {
      setLoading(false);
    }
  }, [onError]);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 30000);
    return () => clearInterval(timer);
  }, [refresh]);

  const updateSetting = async (patch) => {
    setSaving(true);
    try {
      const { data } = await api.put('/admin/system/settings', patch);
      setSettings(data);
      await refresh();
    } catch (err) {
      onError(errorMessage(err, 'Could not update setting'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="card glass mb-8" style={{ marginBottom: '2rem' }}>
        <h3 className="mb-4">Runtime switches</h3>
        <div className="flex justify-between items-center gap-4" style={{ padding: '12px 0', borderBottom: '1px solid var(--border-color)' }}>
          <div>
            <h4 style={{ margin: 0 }}>Backend self-ping</h4>
            <p className="text-secondary" style={{ fontSize: '0.85rem', margin: 0 }}>
              Pings the backend every 10 minutes so Render's free tier does not go to sleep.
            </p>
          </div>
          <Toggle checked={!!settings?.keepAliveEnabled} disabled={!settings || saving}
            onChange={() => updateSetting({ keepAliveEnabled: !settings.keepAliveEnabled })} />
        </div>
        <div className="flex justify-between items-center gap-4" style={{ padding: '12px 0' }}>
          <div>
            <h4 style={{ margin: 0 }}>Use Redis</h4>
            <p className="text-secondary" style={{ fontSize: '0.85rem', margin: 0 }}>
              Off = no caching and clicks are queued in memory. Turn off if Upstash is down or out of quota.
            </p>
          </div>
          <Toggle checked={!!settings?.redisEnabled} disabled={!settings || saving}
            onChange={() => updateSetting({ redisEnabled: !settings.redisEnabled })} />
        </div>
      </div>

      <div className="card glass mb-8" style={{ marginBottom: '2rem' }}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="flex items-center gap-2" style={{ margin: 0 }}>
            <Activity size={20} /> System health {health && <Badge status={health.status} />}
          </h3>
          <button className="btn btn-secondary" onClick={refresh} disabled={loading}>
            <RefreshCw size={16} /> {loading ? 'Checking…' : 'Refresh'}
          </button>
        </div>
        {health && (
          <p className="text-secondary" style={{ fontSize: '0.8rem' }}>
            Checked {new Date(health.checkedAt).toLocaleTimeString()} · refreshes every 30s · click a row for details
          </p>
        )}

        {health && Object.entries(health.components).map(([key, component]) => (
          <div key={key} style={{ borderBottom: '1px solid var(--border-color)', padding: '10px 0' }}>
            <div className="flex justify-between items-center" style={{ cursor: 'pointer' }}
              onClick={() => setExpanded({ ...expanded, [key]: !expanded[key] })}>
              <span style={{ fontWeight: 500 }}>{LABELS[key] || key}</span>
              <span className="flex items-center gap-2">
                {component.latencyMs != null && <span className="text-secondary" style={{ fontSize: '0.8rem' }}>{component.latencyMs} ms</span>}
                <Badge status={component.status} />
              </span>
            </div>
            {expanded[key] && (
              <table style={{ width: '100%', fontSize: '0.8rem', marginTop: '8px', tableLayout: 'fixed' }}>
                <tbody>
                  {Object.entries(component.details || {}).map(([k, v]) => (
                    <tr key={k}>
                      <td className="text-secondary" style={{ width: '40%', padding: '2px 0', verticalAlign: 'top' }}>{k}</td>
                      <td style={{ wordBreak: 'break-word' }}>{formatValue(v)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        ))}

        {health?.requestInfo && (
          <details style={{ marginTop: '12px', fontSize: '0.8rem' }}>
            <summary className="text-secondary" style={{ cursor: 'pointer' }}>Request IP debug (for TRUSTED_PROXY_COUNT)</summary>
            <p className="text-secondary" style={{ margin: '6px 0' }}>
              "resolvedClientIp" should equal your real public IP. If it shows a proxy address, adjust TRUSTED_PROXY_COUNT on Render.
            </p>
            <table style={{ width: '100%', tableLayout: 'fixed' }}>
              <tbody>
                {Object.entries(health.requestInfo).map(([k, v]) => (
                  <tr key={k}>
                    <td className="text-secondary" style={{ width: '40%' }}>{k}</td>
                    <td style={{ wordBreak: 'break-word' }}>{formatValue(v)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </details>
        )}
      </div>
    </>
  );
};

export default SystemHealthPanel;
