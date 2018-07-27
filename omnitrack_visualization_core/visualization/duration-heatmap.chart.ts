import OTChart from './ot_chart'
import { Chart, LinearTickOptions } from 'chart.js'
import IDurationPointDataType from './datatypes/duration-point.datatype'
import {DateOrientedPointType, DurationPoint} from './datatypes/duration-point.datatype'

declare var require: any
var moment = require('moment');
var _s = require("underscore.string");

export default class DurationHeatmap extends OTChart<IDurationPointDataType>{

  private dateOrientedData: Array<DateOrientedPointType>

  onDatasetChanged(data: IDurationPointDataType): void {
    if(data.intrinsicValueRange)
    {
      data.data.forEach(
        d=>{
          d.valueRatio = (d.value - data.intrinsicValueRange.from)/(data.intrinsicValueRange.to - data.intrinsicValueRange.from)
        }
      )
    }
    else{
      //Deal with data min max
    }
    
    const sequence = []
    for(let i=0; i<data.dates.length; i++)
    {
      sequence.push(i)
    }
    this.dateOrientedData = sequence.map(i=>{return {startDate: data.dates[i], dateIndex: i, d: data.data.filter(d=>d.i == i)}})
  }

  onUpdateChartToCanvas(canvasContext: HTMLCanvasElement): void {

    canvasContext.height = (canvasContext.width/13) * Math.max(0.75,(1-0.01*this.data.dates.length)) * (this.data.dates.length + 1.5)

    const chart = new Chart(canvasContext, {
      type: "durationHeatmap",
      options:{
        layout:{
          padding: {left:0, right:0, top:0, bottom:0}
        },
        events: [],
        scales: {
        xAxes: [{
          position: "top",
          ticks:{
            stepSize: 1/24,
            padding: 2,
            callback: function(value, index, values) {
              var hourOfDay = Math.floor(value * 24);
              var minute = Math.round((value * 24 - hourOfDay)*60)
              
              if(minute==60)
              {
                hourOfDay++
                minute = 0
              }

              var result
              if(minute<1)
              {
                result = _s.pad(hourOfDay, 2, '0') + ":00"
              }
              else result = _s.pad(hourOfDay, 2, '0') + ":" + _s.pad(minute, 2, '0')
              return result
            }
          } as LinearTickOptions,
          gridLines: {
            drawTicks: true,
            tickMarkLength: 2,
            drawBorder: false,
          }
        }],
        yAxes: [
          { 
            type: 'category',
            gridLines:{
              display: false, 
              drawBorder: true,
              tickMarkLength: 2,
            },
            ticks: {
              padding:5,
              reverse: true,
            }
          }
        ]
      },
      legend: { display: false }
    },
      data: {
        labels: this.data.dates.map(t=> { return moment.unix(t/1000).format("MMM DD")}),
        datasets: [{ data: this.dateOrientedData }]
      } as any,
    })
  }
  
}