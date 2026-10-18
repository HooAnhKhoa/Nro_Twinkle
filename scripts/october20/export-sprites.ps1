param([string]$Root=(Split-Path (Split-Path $PSScriptRoot -Parent) -Parent))
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$drawingRefs=if($PSVersionTable.PSVersion.Major -lt 6){@('System.Drawing')}else{@('System.Drawing.Common','System.Drawing.Primitives','System.Private.Windows.GdiPlus','System.Private.Windows.Core')}
Add-Type -ReferencedAssemblies $drawingRefs -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
using System.IO;
public static class OctoberSpriteExport {
 static int Cut(Bitmap b,int expected,int radius,bool horizontal,int from,int to){int best=expected,min=int.MaxValue;for(int p=Math.Max(1,expected-radius);p<Math.Min((horizontal?b.Height:b.Width)-1,expected+radius);p++){int score=0;for(int q=from;q<to;q++){var c=horizontal?b.GetPixel(q,p):b.GetPixel(p,q);if(c.A>64)score++;}if(score<min){min=score;best=p;}}return best;}
 static Rectangle Bounds(Bitmap b,Rectangle region){int l=region.Right,t=region.Bottom,r=-1,d=-1;for(int y=region.Top;y<region.Bottom;y++)for(int x=region.Left;x<region.Right;x++)if(b.GetPixel(x,y).A>64){l=Math.Min(l,x);r=Math.Max(r,x);t=Math.Min(t,y);d=Math.Max(d,y);}if(r<l)throw new Exception("Empty frame");return Rectangle.FromLTRB(l,t,r+1,d+1);}
 public static void Export(string input,string output,int first,int last,int bodyFirst,int bodyLast,int portrait){
  Directory.CreateDirectory(output);using(var source=new Bitmap(input)){
   int[] ys={0,0,0,0,source.Height};for(int row=1;row<4;row++)ys[row]=Cut(source,source.Height*row/4,source.Height/32,true,0,source.Width);
   Rectangle[] frames=new Rectangle[20];for(int row=0;row<4;row++){int[] xs={0,0,0,0,0,source.Width};for(int col=1;col<5;col++)xs[col]=Cut(source,source.Width*col/5,source.Width/24,false,ys[row],ys[row+1]);for(int col=0;col<5;col++)frames[row*5+col]=Bounds(source,Rectangle.FromLTRB(xs[col],ys[row],xs[col+1],ys[row+1]));}
   Console.WriteLine(input+" first frame "+frames[0]+"; row cuts "+String.Join(",",ys)); double scale=60.0/Math.Max(frames[0].Height,frames[1].Height);int[] poses={0,2,3,6,7,10,11,12,13,14,15,16,17,18,19,1,4};
   for(int id=first;id<=last;id++){
    using(var baseFrame=new Bitmap(96,80,PixelFormat.Format32bppArgb)){
     if(id>=bodyFirst&&id<=bodyLast){Rectangle frame=frames[poses[(id-bodyFirst)%poses.Length]];int w=(int)Math.Round(frame.Width*scale),h=(int)Math.Round(frame.Height*scale);if(w>94||h>76)throw new Exception("Frame exceeds native canvas");using(var g=Graphics.FromImage(baseFrame)){g.InterpolationMode=InterpolationMode.NearestNeighbor;g.PixelOffsetMode=PixelOffsetMode.Half;g.DrawImage(source,new Rectangle((96-w)/2,72-h,w,h),frame,GraphicsUnit.Pixel);}}
     SaveZooms(baseFrame,output,id);
    }
   }
   using(var icon=new Bitmap(32,32,PixelFormat.Format32bppArgb)){var r=frames[0];using(var g=Graphics.FromImage(icon)){g.InterpolationMode=InterpolationMode.NearestNeighbor;g.DrawImage(source,new Rectangle(2,1,28,30),r,GraphicsUnit.Pixel);}SaveZooms(icon,output,portrait);}
  }
 }
 public static void Icon(string input,string output,int id){using(var source=new Bitmap(input)){Rectangle bounds=Bounds(source,new Rectangle(0,0,source.Width,source.Height));using(var icon=new Bitmap(32,32,PixelFormat.Format32bppArgb)){double scale=Math.Min(30.0/bounds.Width,30.0/bounds.Height);int w=(int)(bounds.Width*scale),h=(int)(bounds.Height*scale);using(var g=Graphics.FromImage(icon)){g.InterpolationMode=InterpolationMode.NearestNeighbor;g.DrawImage(source,new Rectangle((32-w)/2,(32-h)/2,w,h),bounds,GraphicsUnit.Pixel);}SaveZooms(icon,output,id);}}}
 static void SaveZooms(Bitmap bitmap,string output,int id){for(int z=1;z<=4;z++){string dir=Path.Combine(output,"x"+z);Directory.CreateDirectory(dir);using(var target=new Bitmap(bitmap.Width*z,bitmap.Height*z,PixelFormat.Format32bppArgb)){using(var g=Graphics.FromImage(target)){g.InterpolationMode=InterpolationMode.NearestNeighbor;g.PixelOffsetMode=PixelOffsetMode.Half;g.DrawImage(bitmap,new Rectangle(0,0,target.Width,target.Height));}target.Save(Path.Combine(dir,id+".png"),ImageFormat.Png);}}}
}
"@
[OctoberSpriteExport]::Export((Join-Path $Root 'assets/october20/bulma-street-sheet-v1.png'),(Join-Path $Root 'assets/october20/icons'),17577,17603,17579,17590,17604)
[OctoberSpriteExport]::Export((Join-Path $Root 'assets/october20/ly-street-sheet-v1.png'),(Join-Path $Root 'assets/october20/icons'),17606,17632,17608,17619,17633)
'Exported costume frames and portraits at x1–x4.'

[OctoberSpriteExport]::Icon((Join-Path $Root 'assets/october20/rose-box-icon-v1.png'),(Join-Path $Root 'assets/october20/icons'),17740)
foreach($z in 1..4){Copy-Item -LiteralPath (Join-Path $Root ('data/icon/x'+$z+'/13632.png')) -Destination (Join-Path $Root ('assets/october20/icons/x'+$z+'/17710.png')) -Force}
