// Mirrors ChatRequest / ReplyBuilder.ChatReply / ChartData / ChartSeries from the backend exactly.

export interface ChatRequestPayload {
  userId: number;
  message: string;
}

export interface ChartSeries {
  name: string;
  data: number[];
}

export interface ChartData {
  type: 'bar' | 'line' | 'pie';
  labels: string[];
  series: ChartSeries[];
}

export interface ChatReply {
  replyText: string;
  render: 'text' | 'chart';
  chart: ChartData | null;
}

// A message as tracked in the UI's local chat history.
// 'role' distinguishes what the user typed from what the backend sent back.
export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  chart?: ChartData | null;
}
