import { OTChartDefaultOptions } from "./visualization"
export default abstract class OTChart<D>{
  
  protected data: D = null

  private lastWidth: number = 0
  private lastHeight: number = 0

  protected onResized: Array<(width:number, height:number)=>{}> = []

  constructor(protected options?: OTChartDefaultOptions)
  {
    
  }

  setData(data: D){
    this.data = data
    this.onDatasetChanged(data)
  }

  abstract onDatasetChanged(data: D): void

  updateChartToCanvas(canvasContext: string):void{
    const element = document.getElementById(canvasContext) as HTMLCanvasElement
    const mutationObserver = new MutationObserver((record, observer)=>{
        if(this.lastWidth!=element.width || this.lastHeight != element.height)
        {
          this.lastWidth = element.width
          this.lastHeight = element.height
          this.onResized.forEach(l=>l(this.lastWidth, this.lastHeight))
          //console.log("width: " + element.clientWidth + ", " + element.width + ", windowWidth: " + window.outerWidth)
          //console.log("height: " + element.clientHeight + ", " + element.height + ", windowHeight: " + window.outerHeight)
        
        }
    })

    mutationObserver.observe(element, {
      attributes: true, 
      subtree: false, 
      characterData: false,
      childList:false,
      attributeFilter: ["style", "width", "height"]
    })
    this.onUpdateChartToCanvas(element)
  }

  addOnResizedListener(listener: (width:number, height:number)=>{}){
    if(this.onResized.indexOf(listener)==-1)
      this.onResized.push(listener)
  }

  abstract onUpdateChartToCanvas(canvasContext: HTMLCanvasElement):void
}