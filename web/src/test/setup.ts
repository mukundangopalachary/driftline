import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Vitest globals are off, so Testing Library cannot register its own cleanup. Without this the DOM
// from earlier tests stays around and queries match elements from other tests.
afterEach(() => {
  cleanup()
})
