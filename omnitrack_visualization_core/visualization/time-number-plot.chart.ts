import OTChart from './ot_chart'
import { OTChartDefaultOptions } from "./visualization"
import { Chart, LinearTickOptions, TimeScale, TickOptions } from 'chart.js'
import ITimeNumberPlotDataType from './datatypes/time-number-plot.datatype'
export default class TimeNumberPlot extends OTChart<ITimeNumberPlotDataType>{

  private timeOptions: TimeScale = {
    unit: 'day',
    unitStepSize: 1,
    gridLines: {

    }
  }

  private timeTicks: TickOptions = {}

  private dayMillis = 24 * 60 * 60 * 1000

  constructor(protected options?: OTChartDefaultOptions)
  {
    super(options);
    
    this.timeTicks.minRotation = 45
    this.timeTicks.padding = 5
    this.timeOptions.gridLines.offsetGridLines = true
  }

  onDatasetChanged(data: ITimeNumberPlotDataType): void {
    if(data.range)
    {
      if(data.range.from)
        this.timeOptions.min = new Date(data.range.from).toISOString()
      
      if(data.range.to)
        this.timeOptions.max = new Date(data.range.to).toISOString()

        
      if(data.range.from != null && data.range.to != null)
      {
        const rangeLength = data.range.to - data.range.from 
        if(rangeLength >20 * this.dayMillis)
        {
          this.timeOptions.unitStepSize = 2
        }
      }
    }
  }
  onUpdateChartToCanvas(canvasContext: HTMLCanvasElement): void {
    const chart = new Chart(canvasContext, {
      type: "scatter",
      data:{
        datasets:[
          {
            label: this.data.labels.t + " " + this.data.labels.y,
            data: this.data.data.map(d=>{return {t:new Date(d.t), y: d.y}})
          }
        ]
      },
      options: {
        legend: {display: false},
        tooltips: {
          enabled: false
        },
        layout:{
          padding: {left:0, right:0, top:3, bottom:0}
        },
        elements: {
          point: {
            radius: 5,
            borderColor: "rgba(255,255,255,1.0)"
          }
        },
        scales: {
            xAxes: [{
                type: 'time',
                ticks: this.timeTicks,
                gridLines: {
                  tickMarkLength: 10
                },
                time: this.timeOptions
            }],
            yAxes: [{
              type: 'linear',
              ticks: {
                padding: 5,
                beginAtZero: true,
              } as LinearTickOptions,
              gridLines:{
                tickMarkLength: 0
              }
            }]
          }
        }
    })
  }
  
}