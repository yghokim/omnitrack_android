export default interface ITimeNumberPlotDataType{
  labels?:{
    t:string,
    y:string
  },
  data?:Array<{t:number, y: number}>
  range?:{
    from:number,
    to:number,
    timezone:string
  }
}