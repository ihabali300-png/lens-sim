/* runs the depth model off the main thread so the live view stays smooth */
let pipe=null,RI=null,dev='';
async function load(){
  const T=await import('https://cdn.jsdelivr.net/npm/@huggingface/transformers@3.0.2');RI=T.RawImage;
  const tries=(self.navigator&&navigator.gpu)?[['webgpu','fp16'],['wasm','q8']]:[['wasm','q8']];
  for(const [device,dtype] of tries){
    try{
      pipe=await T.pipeline('depth-estimation','onnx-community/depth-anything-v2-small',{device,dtype,
        progress_callback:p=>{if(p.status==='progress'&&p.file&&/\.onnx$/.test(p.file))postMessage({t:'prog',v:Math.round(p.progress||0)})}});
      dev=device;break;
    }catch(e){pipe=null}
  }
  if(!pipe)throw new Error('no backend');
  const fe=pipe.processor&&(pipe.processor.feature_extractor||pipe.processor.image_processor);
  if(fe){const s=dev==='webgpu'?308:224;fe.size={width:s,height:s};fe.crop_size=fe.size}
  postMessage({t:'ready',dev});
}
onmessage=async e=>{
  const m=e.data;
  if(m.t==='load'){try{await load()}catch(err){postMessage({t:'fail',err:String(err)})}return}
  if(m.t==='run'&&pipe){
    try{
      const img=new RI(new Uint8ClampedArray(m.buf),m.w,m.h,4).rgb();
      const t0=performance.now(),d=(await pipe(img)).depth;
      const data=new Uint8Array(d.data);
      postMessage({t:'depth',w:d.width,h:d.height,ch:d.channels||1,buf:data.buffer,ms:performance.now()-t0},[data.buffer]);
    }catch(err){postMessage({t:'fail',err:String(err)})}
  }
};
