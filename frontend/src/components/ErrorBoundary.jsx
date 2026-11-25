import { Component } from 'react';

export default class ErrorBoundary extends Component {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  render() {
    if (!this.state.failed) return this.props.children;

    return (
      <main role="alert" className="card glass text-center" style={{ maxWidth: '440px', margin: '4rem auto' }}>
        <h1 className="mb-4">Linkly couldn’t load this page</h1>
        <p className="text-secondary mb-6">Try reloading the page.</p>
        <button type="button" className="btn btn-primary" onClick={() => window.location.reload()}>
          Reload page
        </button>
      </main>
    );
  }
}
