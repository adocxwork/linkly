import { useCallback, useEffect, useState } from 'react';
import { Activity, RefreshCw } from 'lucide-react';
import api, { errorMessage } from '../api';

const services = [
  ['database', 'Database'],
  ['redis', 'Click queue'],
  ['clickPipeline', 'Analytics processing'],
  ['geoIp', 'Location analytics'],
];

const labels = {
  UP: 'Operational',
  DEGRADED: 'Limited',
  DOWN: 'Unavailable',
  DISABLED: 'Off',
  PENDING: 'Waiting for traffic',
};

function Status({ value }) {
  return <span className={`service-status status-${value.toLowerCase()}`}>{labels[value] || 'Unknown'}</span>;
}

function Setting({ title, description, checked, onChange, disabled }) {
  return (
    <label className="service-setting">
      <span><strong>{title}</strong><small>{description}</small></span>
      <input className="service-switch" type="checkbox" role="switch" checked={checked}
        onChange={onChange} disabled={disabled} aria-label={title} />
    </label>
  );
}

export default function SystemHealthPanel({ onError }) {
  const [health, setHealth] = useState(null);
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [saving, setSaving] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [healthResponse, settingsResponse] = await Promise.all([
        api.get('/admin/system/health'), api.get('/admin/system/settings'),
      ]);
      setHealth(healthResponse.data);
      setSettings(settingsResponse.data);
      setLoadFailed(false);
    } catch {
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 30_000);
    return () => clearInterval(timer);
  }, [refresh]);

  async function updateSetting(patch) {
    setSaving(true);
    try {
      const { data } = await api.put('/admin/system/settings', patch);
      setSettings(data);
      await refresh();
    } catch (err) {
      onError(errorMessage(err, 'Could not update the service setting.'));
    } finally {
      setSaving(false);
    }
  }

  const settingsDisabled = !settings || saving || loading;

  return (
    <div className="admin-service-grid">
      <section className="card health-card" aria-labelledby="health-title">
        <div className="health-heading">
          <span className="feature-icon"><Activity size={21} aria-hidden="true" /></span>
          <div>
            <h2 id="health-title" className="section-title">System health</h2>
            <p>{loadFailed ? 'Couldn’t refresh the service status.' : health
              ? `Checked at ${new Date(health.checkedAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}`
              : 'Checking services…'}</p>
          </div>
          <button type="button" className="btn btn-secondary health-refresh" onClick={refresh}
            disabled={loading || saving} aria-label="Refresh service status">
            <RefreshCw size={16} aria-hidden="true" /> <span>Refresh</span>
          </button>
        </div>

        {health && (
          <>
            <div className="health-summary">
              <span>Overall status</span>
              <Status value={health.status} />
            </div>
            <ul className="health-services">
              {services.map(([key, label]) => {
                const status = health.components?.[key]?.status;
                return status && (
                  <li key={key}><span>{label}</span><Status value={status} /></li>
                );
              })}
            </ul>
          </>
        )}
      </section>

      <section className="card" aria-labelledby="service-settings-title">
        <h2 id="service-settings-title" className="section-title mb-4">Service settings</h2>
        <Setting title="Keep backend awake" description="Reduce cold starts while the backend is running."
          checked={!!settings?.keepAliveEnabled} disabled={settingsDisabled}
          onChange={() => updateSetting({ keepAliveEnabled: !settings.keepAliveEnabled })} />
        <Setting title="Use Redis for click events" description="Use the shared queue when Redis is available."
          checked={!!settings?.redisEnabled} disabled={settingsDisabled}
          onChange={() => updateSetting({ redisEnabled: !settings.redisEnabled })} />
      </section>
    </div>
  );
}
