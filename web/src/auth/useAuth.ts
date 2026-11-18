import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../lib/api.ts'

export type User = {
  id: string
  githubLogin: string
  name: string | null
  avatarUrl: string | null
}

const ME_KEY = ['me'] as const

/** The signed-in user, or null when signed out. */
export function useCurrentUser() {
  return useQuery({
    queryKey: ME_KEY,
    queryFn: async (): Promise<User | null> => {
      const response = await apiFetch('/api/auth/me')
      if (response.status === 401) return null
      if (!response.ok) throw new Error(`Could not load the current user (${response.status})`)
      return (await response.json()) as User
    },
    retry: false,
    staleTime: 60_000,
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      await apiFetch('/api/auth/logout', { method: 'POST' })
    },
    onSettled: () => {
      // Drop everything cached for the old user, then record that nobody is signed in.
      queryClient.clear()
      queryClient.setQueryData(ME_KEY, null)
    },
  })
}
