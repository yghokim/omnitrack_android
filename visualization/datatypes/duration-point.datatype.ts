export default interface IDurationPointDataType{
  dates:Array<number>,
  intrinsicValueRange?: {from:number, to: number, level: number},
  data: Array<DurationPoint>
}

export interface DurationPoint{i:number, fromRatio:number, toRatio:number, value: number, valueRatio?: number, cutL:boolean, cutR: boolean}

export interface DateOrientedPointType{startDate:number, dateIndex: number, d:Array<DurationPoint>}