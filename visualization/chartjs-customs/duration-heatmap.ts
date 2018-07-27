import { Chart, LinearTickOptions, LinearScale, ChartRectangleOptions } from 'chart.js'
import IDurationPointDataType from '../datatypes/duration-point.datatype'
import { DurationPoint, DateOrientedPointType } from '../datatypes/duration-point.datatype'
import * as canvasHelper from '../helpers/canvas.helper'

declare var require: any
var tinycolor = require('tinycolor2')
interface DurationBarData { centerX: number, centerY: number, width: number, height: number, value: number, valueRatio: number, cutL: boolean, cutR: boolean }
interface BackDropBarData { x: number, centerY: number, width: number, height: number }
export function registerDurationChart() {

  var chart = Chart as any
  var custom: any = chart.controllers.horizontalBar.extend({
    name: "durationHeatmap",
    draw: function (ease: any) {
      const rectangleOptions: ChartRectangleOptions = this.chart.config.options.elements.rectangle
      // Call super method first

      // Now we can do some custom drawing for this dataset. Here we'll draw a red box around the first point in each dataset
      const durationBars: Array<DurationBarData> = this.getMeta().durationBarData
      const backDropBars: Array<BackDropBarData> = this.getMeta().backDropBarData
      const canvasContext: CanvasRenderingContext2D = this.chart.ctx

      backDropBars.forEach(
        b => {
          const easedHeight = b.height * (Math.min(1, ease * 2))
          canvasContext.fillStyle = "rgba(0,0,0,0.08)"
          canvasContext.strokeStyle = null
          canvasContext.fillRect(b.x, b.centerY - easedHeight * .5, b.width, easedHeight)
        }
      )

      durationBars.forEach(
        b => {
          const easedWidth = (b.width - 1) * Math.min(1, Math.max(0, (ease - 0.5)) * 2)
          const color = tinycolor(rectangleOptions.backgroundColor.toString())
          const originalAlpha = color.getAlpha()
          color.setAlpha(originalAlpha * b.valueRatio)
          canvasContext.fillStyle = color.toRgbString()

          var corners: any
          if (b.cutL) {
            corners = { lt: false, lb: false }
          }

          if (b.cutR) {
            corners = { rt: false, rb: false }
          }

          canvasHelper.fillRoundRect(canvasContext, b.centerX, b.centerY, easedWidth, b.height, 3, corners)
          //canvasContext.fillRect(b.centerX - easedWidth * .5, b.centerY - b.height * .5, easedWidth, b.height)
        }
      )
    },
    update: function () {
      const dateScale = this.getIndexScale()
      const timelineScale = this.getValueScale()
      const data: Array<DateOrientedPointType> = this._data
      var min = 1
      var max = 0

      const reverse = dateScale.options.ticks.reverse == true
      if (reverse == true && dateScale.reverseHandled != true) {
        dateScale.ticks.reverse()
        dateScale._ticks.reverse()
        dateScale.reverseHandled = true
      }

      data.forEach(d => {
        d.d.forEach(p => {
          min = Math.min(min, p.fromRatio)
          max = Math.max(max, p.toRatio)
        })
      })
      if (min == 1) {
        timelineScale.min = 0
      } else timelineScale.min = min

      if (max == 0) {
        timelineScale.max = 1
      } else timelineScale.max = max
      timelineScale.buildTicks()

      timelineScale._ticks = timelineScale.ticks.map(function (t: any) { return { label: timelineScale.options.ticks.callback(t), major: t } })
      timelineScale.ticksAsNumbers = timelineScale.ticks.slice();

      const datePixelHeight = dateScale.getPixelForValue(0, 1, 0) - dateScale.getPixelForValue(0, 0, 0)
      const durationBarData: Array<DurationBarData> = []
      const backDropBarData: Array<BackDropBarData> = []
      data.forEach((d, i) => {
        var dateIndex
        if (reverse == true) {
          dateIndex = data.length - d.dateIndex - 1
        } else dateIndex = d.dateIndex


        const centerY = dateScale.getPixelForValue(null, dateIndex, 0)
        if (d.d.length > 0) {
          backDropBarData.push({ x: timelineScale.left, centerY: centerY, width: timelineScale.right - timelineScale.left, height: datePixelHeight * 0.8 })
          d.d.forEach(durationElm => {
            const fromX = timelineScale.getPixelForValue(durationElm.fromRatio, 0, 0)
            const toX = timelineScale.getPixelForValue(durationElm.toRatio, 0, 0)
            const centerX = (toX + fromX) * .5
            durationBarData.push({ centerX: centerX, centerY: centerY, width: toX - fromX, height: datePixelHeight * 0.8, value: durationElm.value, valueRatio: durationElm.valueRatio, cutL: durationElm.cutL, cutR: durationElm.cutR })
          })
        }
      })

      this.getMeta().durationBarData = durationBarData
      this.getMeta().backDropBarData = backDropBarData
    }
  })

  chart.controllers.durationHeatmap = custom;
  chart.defaults.durationHeatmap = chart.defaults.horizontalBar
}