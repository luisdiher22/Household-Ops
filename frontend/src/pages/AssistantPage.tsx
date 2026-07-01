import { useState } from 'react'
import { useAssistantQuery } from '../hooks/useAssistant'
import type { AssistantQueryResponse } from '../types'

interface Exchange {
  question: string
  response?: AssistantQueryResponse
  error?: string
}

const SUGGESTIONS = [
  'What does the house need before Friday?',
  "Do I have anything waiting on my approval right now?",
  "What's on the shopping list that hasn't been bought yet?",
]

export function AssistantPage() {
  const assistantQuery = useAssistantQuery()
  const [question, setQuestion] = useState('')
  const [history, setHistory] = useState<Exchange[]>([])

  function ask(q: string) {
    if (!q.trim() || assistantQuery.isPending) return
    setQuestion('')
    assistantQuery.mutate(q, {
      onSuccess: (response) => setHistory((prev) => [...prev, { question: q, response }]),
      onError: () => setHistory((prev) => [...prev, { question: q, error: 'Something went wrong asking that.' }]),
    })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-navy">Assistant</h1>
        <p className="text-sm text-navy/60">Ask about inventory, tasks, approvals, or the shopping list -- grounded in your household's live data.</p>
      </div>

      {history.length === 0 && (
        <div className="flex flex-wrap gap-2">
          {SUGGESTIONS.map((s) => (
            <button
              key={s}
              onClick={() => ask(s)}
              className="rounded-full border border-navy/30 px-3 py-1.5 text-sm text-navy/70 hover:bg-ivory"
            >
              {s}
            </button>
          ))}
        </div>
      )}

      <div className="space-y-4">
        {history.map((exchange, i) => (
          <div key={i} className="space-y-2">
            <div className="ml-auto w-fit max-w-lg rounded-lg bg-navy px-4 py-2 text-sm text-white">{exchange.question}</div>

            {exchange.error && <div className="w-fit max-w-lg rounded-lg bg-red-50 px-4 py-2 text-sm text-red-700">{exchange.error}</div>}

            {exchange.response && (
              <div className="w-fit max-w-lg space-y-2 rounded-lg border border-navy/15 bg-white px-4 py-3">
                <p className="whitespace-pre-line text-sm text-navy">{exchange.response.answer}</p>
                {exchange.response.toolCalls.length > 0 && (
                  <div className="border-t border-navy/10 pt-2">
                    <p className="mb-1 text-xs font-medium uppercase tracking-wide text-navy/40">Tools used</p>
                    <ul className="space-y-0.5">
                      {exchange.response.toolCalls.map((call, j) => (
                        <li key={j} className="font-mono text-xs text-navy/60">
                          {call.tool}({call.inputSummary})
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
        {assistantQuery.isPending && <div className="w-fit rounded-lg border border-navy/15 bg-white px-4 py-2 text-sm text-navy/40">Thinking...</div>}
      </div>

      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          ask(question)
        }}
      >
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question about the household..."
          className="flex-1 rounded border border-navy/30 px-3 py-2 text-sm"
        />
        <button
          type="submit"
          disabled={assistantQuery.isPending}
          className="rounded bg-navy px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
        >
          Ask
        </button>
      </form>
    </div>
  )
}
