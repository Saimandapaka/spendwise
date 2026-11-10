import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { ChatMessage } from '../models/chat.model';

@Component({
  selector: 'app-chat-message',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './chat-message.component.html',
  styleUrl: './chat-message.component.css',
})
export class ChatMessageComponent {
  @Input({ required: true }) message!: ChatMessage;

  get chartJsType(): ChartType {
    return (this.message.chart?.type ?? 'bar') as ChartType;
  }

  get chartData(): ChartConfiguration['data'] {
    const chart = this.message.chart;
    if (!chart) return { labels: [], datasets: [] };

    return {
      labels: chart.labels,
      datasets: chart.series.map((s) => ({
        label: s.name,
        data: s.data,
      })),
    };
  }

  get chartOptions(): ChartConfiguration['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: (this.message.chart?.series?.length ?? 0) !== 1 },
      },
    };
  }
}
