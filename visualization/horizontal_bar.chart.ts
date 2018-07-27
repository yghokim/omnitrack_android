import OTChart from './ot_chart'
import { Chart, LinearTickOptions } from 'chart.js'
import IBarChartDataType from './datatypes/bar-chart.datatype'
export default class HorizontalBarChart extends OTChart<IBarChartDataType>{


  onDatasetChanged(data: IBarChartDataType): void {

  }

  onUpdateChartToCanvas(canvasContext: HTMLCanvasElement): void {
    canvasContext.height = (canvasContext.width/12) * this.data.data.length
    const chart = new Chart(canvasContext, {
      type: "horizontalBar",
      options: {
        layout:{
          padding: {left:0, right:0, top:0, bottom:0}
        },
        scales: {
          xAxes: [{
            ticks: {
              beginAtZero: true
            } as any,
            gridLines: {
              drawBorder: false, 
              zeroLineWidth: 0 
            }
          }],
          yAxes: [
            { 
              gridLines:{
                display: false, 
                drawBorder: false
              }
            }
          ]
        },
        legend: { display: false }
      },
      data: {
        labels: this.data.data.map(d => d.label),
        datasets: [{ data: this.data.data.map(d => d.value) }]
      },
    })
  }
}