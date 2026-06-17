export const SUBREDDIT_URL = /^https?:\/\/(www\.|old\.)?reddit\.com\/r\/pokemontrades\//i

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
