export function fillRoundRect(ctx:CanvasRenderingContext2D, centerX:number, centerY:number, w:number, h:number, radius:number , corners:{lt?:boolean, rt?:boolean, rb?:boolean, lb?:boolean}=null)
{
  if(w <= 0.00001 || h <= 0.00001 )
  {
    return
  }

  const adjustedRadius = Math.min(radius, w/2, h/2)
  const horLineWidth = w - 2*adjustedRadius
  const verLineHeight = h - 2*adjustedRadius
  const top = centerY - h/2
  const left = centerX - w/2
  const right = centerX + w/2
  const bottom = centerY + h/2
  
  ctx.beginPath()
  if(corners!=null && corners.lt==false)
  {
    ctx.moveTo(left, top)
  }else{
    ctx.moveTo(centerX - horLineWidth/2, top)
  }
  
  if(corners!=null && corners.rt==false)
  {
    ctx.lineTo(right, top)
  }
  else{
    ctx.lineTo(centerX + horLineWidth/2, top)
    ctx.arcTo(right, top, right, centerY - verLineHeight/2, adjustedRadius)
  }

  if(corners!=null && corners.rb==false)
  {
    ctx.lineTo(right, bottom)
  }
  else{
    ctx.lineTo(right, centerY + verLineHeight/2)
    ctx.arcTo(right, bottom, centerX + horLineWidth/2, bottom, adjustedRadius)
  }

  if(corners!=null && corners.lb==false)
  {
    ctx.lineTo(left, bottom)
  }
  else{
    ctx.lineTo(centerX - horLineWidth/2, bottom)
    ctx.arcTo(left, bottom, left, centerY + verLineHeight/2, adjustedRadius)
  }

  if(corners!=null && corners.lt==false)
  {
    ctx.lineTo(left,top)
  }
  else{
    ctx.lineTo(left, centerY - verLineHeight/2)
    ctx.arcTo(left, top, centerX - horLineWidth/2, top, adjustedRadius)
  }
  ctx.closePath()
  ctx.fill()
}
