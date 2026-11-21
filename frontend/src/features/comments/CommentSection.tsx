import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Trash2, MessageSquare } from 'lucide-react'
import { commentApi } from '../../api/endpoints'
import { Button, Field, Textarea, ErrorBanner, Spinner, EmptyState } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import type { Comment } from '../../lib/types'

interface CommentSectionProps {
  taskId: string
}

export function CommentSection({ taskId }: CommentSectionProps) {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [content, setContent] = useState('')

  const commentsQuery = useQuery({
    queryKey: ['comments', taskId],
    queryFn: () => commentApi.list(taskId),
  })

  const createComment = useMutation({
    mutationFn: () => commentApi.create(taskId, content),
    onSuccess: () => {
      toast.success('Comment added')
      setContent('')
      queryClient.invalidateQueries({ queryKey: ['comments', taskId] })
      queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not add comment')),
  })

  const updateComment = useMutation({
    mutationFn: ({ commentId, content }: { commentId: string; content: string }) =>
      commentApi.update(taskId, commentId, content),
    onSuccess: () => {
      toast.success('Comment updated')
      queryClient.invalidateQueries({ queryKey: ['comments', taskId] })
      queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update comment')),
  })

  const deleteComment = useMutation({
    mutationFn: (commentId: string) => commentApi.remove(taskId, commentId),
    onSuccess: () => {
      toast.success('Comment deleted')
      queryClient.invalidateQueries({ queryKey: ['comments', taskId] })
      queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not delete comment')),
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!content.trim()) return
    createComment.mutate()
  }

  function handleEdit(comment: Comment) {
    const newContent = prompt('Edit comment:', comment.content)
    if (newContent !== null && newContent.trim() !== '') {
      updateComment.mutate({ commentId: comment.id, content: newContent.trim() })
    }
  }

  if (commentsQuery.isLoading) {
    return <Spinner />
  }

  return (
    <div className="space-y-4">
      <h3 className="text-sm font-bold text-surface-700 flex items-center gap-2">
        <MessageSquare className="h-4 w-4" />
        Comments
      </h3>

      {commentsQuery.isError && (
        <ErrorBanner message={extractErrorMessage(commentsQuery.error, 'Could not load comments')} />
      )}

      {commentsQuery.data && commentsQuery.data.length === 0 && (
        <EmptyState title="No comments yet" description="Be the first to comment on this task." />
      )}

      <div className="space-y-3">
        {commentsQuery.data?.map((comment) => (
          <div key={comment.id} className="rounded-xl border border-surface-200/60 bg-white/60 backdrop-blur-sm p-4">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className="h-8 w-8 rounded-full bg-gradient-to-br from-brand-100 to-indigo-100 flex items-center justify-center text-xs font-bold text-brand-700">
                  {comment.userFirstName[0]}{comment.userLastName[0]}
                </div>
                <div>
                  <p className="text-sm font-semibold text-surface-800">
                    {comment.userFirstName} {comment.userLastName}
                  </p>
                  <p className="text-xs text-surface-400">
                    {new Date(comment.createdAt).toLocaleString()}
                    {comment.updatedAt !== comment.createdAt && ' (edited)'}
                  </p>
                </div>
              </div>
              {comment.userId === user?.id && (
                <div className="flex gap-1">
                  <button
                    onClick={() => handleEdit(comment)}
                    className="text-xs text-surface-400 hover:text-brand-700 transition-colors px-2 py-1 rounded-lg hover:bg-brand-50"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => deleteComment.mutate(comment.id)}
                    className="text-xs text-surface-400 hover:text-rose-600 transition-colors px-2 py-1 rounded-lg hover:bg-rose-50"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              )}
            </div>
            <p className="text-sm text-surface-700 whitespace-pre-wrap">{comment.content}</p>
          </div>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="space-y-2">
        <Field label="Add a comment">
          <Textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Write a comment..."
            rows={2}
          />
        </Field>
        <div className="flex justify-end">
          <Button
            type="submit"
            size="sm"
            loading={createComment.isPending}
            disabled={!content.trim()}
          >
            Post comment
          </Button>
        </div>
        {createComment.isError && (
          <ErrorBanner message={extractErrorMessage(createComment.error, 'Could not add comment')} />
        )}
      </form>
    </div>
  )
}
