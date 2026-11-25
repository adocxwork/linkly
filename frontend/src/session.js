export function readStoredUser() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    return typeof user?.username === 'string' && user.username ? user : null;
  } catch {
    return null;
  }
}
