// import React, { useState, useRef } from "react";
// import api from "./api/axios";
// import "./App.css";

// // --- Matrix Overlay Component ---
// const MatrixOverlay = ({ linearData, width, height, pos, title, isLocked }) => {
//   if (!linearData || !pos || width === 0) {
//     return <div className="matrix-viewer empty">HOVER OR CLICK {title}</div>;
//   }

//   const radius = 2;
//   const px = Math.floor(pos.x); 
//   const py = Math.floor(pos.y);
//   const grid = [];

//   for (let dy = -radius; dy <= radius; dy++) {
//     const row = [];
//     for (let dx = -radius; dx <= radius; dx++) {
//       const x = px + dx; 
//       const y = py + dy;

//       if (x >= 0 && x < width && y >= 0 && y < height) {
//         const val = linearData[y * width + x];
//         if (typeof val === 'object' && val !== null) {
//           row.push(val);
//         } else if (typeof val === 'number') {
//           row.push({
//             r: (val >> 16) & 0xFF,
//             g: (val >> 8) & 0xFF,
//             b: val & 0xFF
//           });
//         } else {
//           row.push(null);
//         }
//       } else {
//         row.push(null);
//       }
//     }
//     grid.push(row);
//   }

//   return (
//     <div className={`matrix-viewer ${isLocked ? 'locked' : ''}`}>
//       <div className="matrix-header">
//         <span className="matrix-title">{title}</span>
//         <span className="matrix-coords">[{px}, {py}] {isLocked && "🔒"}</span>
//       </div>
//       <div className="matrix-grid">
//         {grid.map((row, i) => (
//           <div key={i} className="matrix-row">
//             {row.map((pixel, j) => (
//               <div key={j} className="matrix-cell rgb-cell" style={{ 
//                 backgroundColor: pixel ? `rgb(${pixel.r},${pixel.g},${pixel.b})` : '#111',
//                 color: (pixel && (pixel.r + pixel.g + pixel.b)/3 > 128) ? '#000' : '#fff',
//                 border: (i===2 && j===2) ? '2px solid #6366f1' : '1px solid #333'
//               }}>
//                 {pixel ? (
//                   <div className="rgb-values">
//                     <span>{pixel.r}</span>
//                     <span>{pixel.g}</span>
//                     <span>{pixel.b}</span>
//                   </div>
//                 ) : '--'}
//               </div>
//             ))}
//           </div>
//         ))}
//       </div>
//     </div>
//   );
// };

// export default function App() {
//   const [originalUrl, setOriginalUrl] = useState(null);
//   const [previewUrl, setPreviewUrl] = useState(null);
//   const [originalBlob, setOriginalBlob] = useState(null);
//   const [sourceLinear, setSourceLinear] = useState(null);
//   const [resultLinear, setResultLinear] = useState(null);
//   const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
//   const [resultDimensions, setResultDimensions] = useState({ width: 0, height: 0 });
//   const [loading, setLoading] = useState(false);
//   const [mousePos, setMousePos] = useState(null);
//   const [lockedPos, setLockedPos] = useState(null);
//   const [isSquare, setIsSquare] = useState(null);

//   const [brightness, setBrightness] = useState(0);
//   const [contrast, setContrast] = useState(0);
//   const [blurIntensity, setBlurIntensity] = useState(0);
//   const [sharpenIntensity, setSharpenIntensity] = useState(0);
//   const [rotateAngle, setRotateAngle] = useState(0);
//   const [zoom, setZoom] = useState(1);
//   const [isGrayscaleMode, setIsGrayscaleMode] = useState(false);
  
//   const [cropData, setCropData] = useState({ x: 0, y: 0, w: 0, h: 0 });
//   const [isDragging, setIsDragging] = useState(false);
//   const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  
//   const fileInputRef = useRef(null);

//   const handleUpload = (e) => {
//     const file = e.target.files[0];
//     if (!file) return;
//     setOriginalBlob(file);
//     const url = URL.createObjectURL(file);
//     setOriginalUrl(url);

//     const img = new Image();
//     img.onload = async () => {
//       setDimensions({ width: img.width, height: img.height });
//       setResultDimensions({ width: img.width, height: img.height });
//       setCropData({ x: 0, y: 0, w: 0, h: 0 });
      
//       const fd = new FormData();
//       fd.append("image", file);
//       fd.append("grayscale", false);
//       fd.append("level", 0);

//       try {
//         const res = await api.post("/brightness", fd);
//         setSourceLinear(res.data.linear);
//         setResultLinear(res.data.linear);
//         checkIfSquare(file);
//       } catch (err) { console.error(err); }
//     };
//     img.src = url;
//     setPreviewUrl(null);
//     setLockedPos(null);
//     resetToolStates();
//   };

//   const handleToggleGrayscale = async () => {
//     const newMode = !isGrayscaleMode;
//     setIsGrayscaleMode(newMode);
    
//     if (originalBlob) {
//       setLoading(true);
//       const fd = new FormData();
//       fd.append("image", originalBlob);
//       fd.append("grayscale", newMode);
//       fd.append("level", 0); 

//       try {
//         const res = await api.post("/brightness", fd);
//         setSourceLinear(res.data.linear);
//         processImage('brightness', { level: brightness }); 
//       } catch (err) { console.error(err); } 
//       finally { setLoading(false); }
//     }
//   };

//   const checkIfSquare = async (file) => {
//     const fd = new FormData();
//     fd.append("image", file);
//     try {
//       const res = await api.post("/is-square", fd);
//       setIsSquare(res.data);
//     } catch (err) { console.error(err); }
//   };

//   const resetToolStates = () => {
//     setBrightness(0); setContrast(0); setBlurIntensity(0);
//     setSharpenIntensity(0); setRotateAngle(0); setZoom(1);
//     setIsGrayscaleMode(false); setCropData({ x: 0, y: 0, w: 0, h: 0 });
//   };

//   const handleResetAll = () => {
//     if (!originalBlob) return;
//     resetToolStates();
//     setPreviewUrl(null);
//     setResultLinear(null);
//     setLockedPos(null);
//     const img = new Image();
//     img.onload = () => {
//         setDimensions({ width: img.width, height: img.height });
//         setResultDimensions({ width: img.width, height: img.height });
//     };
//     img.src = originalUrl;
//   };

// const processImage = async (endpoint, params = {}) => {
//     if (!originalBlob) return;

//     if (!isGrayscaleMode && zoom === 1 && (endpoint === 'blur' || endpoint === 'sharpen') && params.intensity === 0) {
//         setPreviewUrl(null);
//         setResultLinear(null);
//         setResultDimensions({ width: dimensions.width, height: dimensions.height });
//         return;
//     }

//     setLoading(true);
//     const fd = new FormData();
//     fd.append("image", originalBlob);
//     fd.append("grayscale", isGrayscaleMode); // Ensure grayscale state is always sent
    
//     // Maintain current zoom scale for all filter operations
//     if (endpoint !== 'zoom' && zoom !== 1) {
//         fd.append("scale", zoom);
//     }

//     Object.entries(params).forEach(([key, val]) => fd.append(key, val));

//     try {
//       const res = await api.post(`${endpoint}`, fd);
//       setPreviewUrl(`data:image/png;base64,${res.data.image}`);
//       setResultLinear(res.data.linear);
      
//       // Synchronize dimensions from the server response
//       setResultDimensions({ 
//           width: res.data.width, 
//           height: res.data.height 
//       });

//       if (endpoint === 'crop') {
//           setDimensions({ width: res.data.width, height: res.data.height });
//       }
//     } catch (err) { 
//       console.error("Filter application failed:", err); 
//     } finally { 
//       setLoading(false); 
//     }
//   };

//   const handleMouseDown = (e) => {
//     const rect = e.currentTarget.getBoundingClientRect();
//     const x = Math.floor(e.clientX - rect.left);
//     const y = Math.floor(e.clientY - rect.top);
//     setStartPos({ x, y });
//     setIsDragging(true);
//   };

//   const handleMouseMoveViewport = (e, isResult) => {
//     const rect = e.currentTarget.getBoundingClientRect();
//     const x = Math.floor(e.clientX - rect.left);
//     const y = Math.floor(e.clientY - rect.top);
//     setMousePos({ x, y });

//     if (isDragging && !isResult) {
//       const left = Math.max(0, Math.min(startPos.x, x));
//       const top = Math.max(0, Math.min(startPos.y, y));
//       const width = Math.min(dimensions.width - left, Math.abs(x - startPos.x));
//       const height = Math.min(dimensions.height - top, Math.abs(y - startPos.y));
//       setCropData({ x: left, y: top, w: width, h: height });
//     }
//   };

//   const toggleLock = () => {
//     if (lockedPos) setLockedPos(null);
//     else setLockedPos({ ...mousePos });
//   };

//   return (
//     <div className="pixel-app">
//       <header className="brand-header">
//         <div className="logo">PIXEL<span>GREY</span>.v1</div>
//         <div className="header-controls">
//             {isSquare !== null && (
//                 <div className={`square-badge ${isSquare ? 'is-square' : 'not-square'}`}>
//                     {isSquare ? "■ SQUARE_MATCH" : "▭ ASYMMETRIC"}
//                 </div>
//             )}
//             <button className={`btn-secondary ${isGrayscaleMode ? 'active' : ''}`} onClick={handleToggleGrayscale}>
//                 {isGrayscaleMode ? "MODE: GRAYSCALE" : "MODE: COLOR"}
//             </button>
//         </div>
//         <div className="status-indicator" style={{color: loading ? 'orange' : '#10b981'}}>
//             {loading ? "BUSY" : "SYSTEM_READY"}
//         </div>
//       </header>

//       <nav className="toolbar">
//         <div className="tool-unit">
//           <span className="tool-label">Analysis</span>
//           <div className="status-box">
//              <div className="stat-row">
//                 <span>Aspect:</span>
//                 <span style={{color: isSquare ? '#10b981' : '#f87171'}}>{isSquare ? "1:1 Match" : "Non-Square"}</span>
//              </div>
//              <button className="btn-secondary" onClick={() => checkIfSquare(originalBlob)}>Re-Validate</button>
//           </div>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Source</span>
//           <button onClick={() => fileInputRef.current.click()}>Upload</button>
//           <input type="file" ref={fileInputRef} onChange={handleUpload} hidden />
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Adjustments</span>
//           <div className="slider-group">
//             <div className="slider-item">
//                 <span>Bri({brightness})</span>
//                 <input type="range" min="-100" max="100" value={brightness} onChange={e => setBrightness(+e.target.value)} onMouseUp={() => processImage('brightness', { level: brightness })} />
//             </div>
//             <div className="slider-item">
//                 <span>Con({contrast})</span>
//                 <input type="range" min="-100" max="100" value={contrast} onChange={e => setContrast(+e.target.value)} onMouseUp={() => processImage('contrast', { level: contrast })} />
//             </div>
//           </div>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Filters</span>
//           <div className="slider-group">
//             <div className="slider-item">
//                 <span>Blur({blurIntensity})</span>
//                 <input type="range" min="0" max="20" value={blurIntensity} onChange={e => setBlurIntensity(+e.target.value)} onMouseUp={() => processImage('blur', { intensity: blurIntensity })} />
//             </div>
//             <div className="slider-item">
//                 <span>Sharp({sharpenIntensity})</span>
//                 <input type="range" min="0" max="20" value={sharpenIntensity} onChange={e => setSharpenIntensity(+e.target.value)} onMouseUp={() => processImage('sharpen', { intensity: sharpenIntensity })} />
//             </div>
//           </div>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Transform</span>
//           <div className="input-group">
//             <input type="number" className="number-input" value={rotateAngle} onChange={(e) => setRotateAngle(e.target.value)} />
//             <button onClick={() => processImage('rotate', { angle: rotateAngle })}>Rotate</button>
//           </div>
//           <div className="btn-group" style={{marginTop:'5px'}}>
//             <button onClick={() => processImage('flip/horizontal')}>Flip H</button>
//             <button onClick={() => processImage('flip/vertical')}>Flip V</button>
//           </div>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">SubMatrix</span>
//           <div className="crop-stats" style={{fontSize: '11px', color: '#6366f1', fontWeight: 'bold'}}>
//             POS: {cropData.x}, {cropData.y} <br/> AREA: {cropData.w}x{cropData.h}
//           </div>
//           <button className="btn-primary" style={{width:'100%', marginTop:'5px'}} onClick={() => processImage('crop', cropData)}>Apply Crop</button>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Zoom Result ({zoom}x)</span>
//           <input type="range" min="1" max="4" step="1" value={zoom} onChange={e => setZoom(+e.target.value)} onMouseUp={() => processImage('zoom', { scale: zoom })} />
//           <div className="btn-group" style={{marginTop:'5px'}}>
//             <button className="btn-secondary" onClick={() => setLockedPos(null)}>Unlock</button>
//             <button className="btn-secondary" onClick={handleResetAll}>Reset All</button>
//           </div>
//         </div>
//       </nav>

//       <main className="workspace">
//         <section className="panel">
//           <MatrixOverlay linearData={sourceLinear} width={dimensions.width} height={dimensions.height} pos={lockedPos || mousePos} title="SOURCE" isLocked={!!lockedPos} />
//           <MatrixOverlay 
//             linearData={resultLinear || sourceLinear} 
//             width={resultDimensions.width} 
//             height={resultDimensions.height} 
//             pos={
//               (lockedPos || mousePos) 
//                 ? { 
//                     x: (lockedPos ? lockedPos.x : mousePos.x) * (resultDimensions.width / dimensions.width), 
//                     y: (lockedPos ? lockedPos.y : mousePos.y) * (resultDimensions.height / dimensions.height) 
//                   } 
//                 : null
//             } 
//             title="RESULT" 
//             isLocked={!!lockedPos} 
//           />
//         </section>

//         <section className="preview-panel">
//           <div className="comparison-view">
//             <div className="viewport-half">
//               <span className="view-tag">ORIGINAL</span>
//               <div className="viewport-container" style={{justifyContent:'flex-start', alignItems:'flex-start'}} 
//                    onClick={toggleLock} 
//                    onMouseDown={handleMouseDown}
//                    onMouseMove={(e) => handleMouseMoveViewport(e, false)} 
//                    onMouseUp={() => setIsDragging(false)}>
//                 {originalUrl && (
//                   <img src={originalUrl} className="preview-img" draggable="false" alt="source" 
//                        style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
//                 )}
//                 {isDragging && <div className="crop-selector-visual" style={{position:'absolute', border:'2px dashed #6366f1', backgroundColor:'rgba(99,102,241,0.2)', left:cropData.x, top:cropData.y, width:cropData.w, height:cropData.h, pointerEvents:'none'}} />}
//               </div>
//             </div>
//             <div className="viewport-half">
//               <span className="view-tag">RESULT</span>
//               <div className="viewport-container" style={{justifyContent:'flex-start', alignItems:'flex-start'}} onClick={toggleLock} onMouseMove={(e) => handleMouseMoveViewport(e, true)}>
//                 {previewUrl ? (
//                   <img src={previewUrl} className="preview-img" draggable="false" alt="result" style={{ imageRendering: 'pixelated' }} />
//                 ) : (
//                   originalUrl && <img src={originalUrl} className="preview-img" draggable="false" alt="source-copy" 
//                                       style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
//                 )}
//               </div>
//             </div>
//           </div>
//         </section>
//       </main>
//     </div>
//   );
// }


// import React, { useState, useRef, useEffect } from "react";
// import api from "./api/axios";
// import "./App.css";

// // --- Matrix Overlay Component ---
// const MatrixOverlay = ({ linearData, width, height, pos, title, isLocked }) => {
//   if (!linearData || !pos || width === 0) {
//     return <div className="matrix-viewer empty">HOVER OR CLICK {title}</div>;
//   }

//   const radius = 2;
//   const px = Math.floor(pos.x); 
//   const py = Math.floor(pos.y);
//   const grid = [];

//   for (let dy = -radius; dy <= radius; dy++) {
//     const row = [];
//     for (let dx = -radius; dx <= radius; dx++) {
//       const x = px + dx; 
//       const y = py + dy;

//       if (x >= 0 && x < width && y >= 0 && y < height) {
//         const val = linearData[y * width + x];
//         if (typeof val === 'object' && val !== null) {
//           row.push(val);
//         } else if (typeof val === 'number') {
//           row.push({
//             r: (val >> 16) & 0xFF,
//             g: (val >> 8) & 0xFF,
//             b: val & 0xFF
//           });
//         } else {
//           row.push(null);
//         }
//       } else {
//         row.push(null);
//       }
//     }
//     grid.push(row);
//   }

//   return (
//     <div className={`matrix-viewer ${isLocked ? 'locked' : ''}`}>
//       <div className="matrix-header">
//         <span className="matrix-title">{title}</span>
//         <span className="matrix-coords">[{px}, {py}] {isLocked && "🔒"}</span>
//       </div>
//       <div className="matrix-grid">
//         {grid.map((row, i) => (
//           <div key={i} className="matrix-row">
//             {row.map((pixel, j) => (
//               <div key={j} className="matrix-cell rgb-cell" style={{ 
//                 backgroundColor: pixel ? `rgb(${pixel.r},${pixel.g},${pixel.b})` : '#111',
//                 color: (pixel && (pixel.r + pixel.g + pixel.b)/3 > 128) ? '#000' : '#fff',
//                 border: (i===2 && j===2) ? '2px solid #6366f1' : '1px solid #333'
//               }}>
//                 {pixel ? (
//                   <div className="rgb-values">
//                     <span>{pixel.r}</span>
//                     <span>{pixel.g}</span>
//                     <span>{pixel.b}</span>
//                   </div>
//                 ) : '--'}
//               </div>
//             ))}
//           </div>
//         ))}
//       </div>
//     </div>
//   );
// };

// // --- Layer Panel Component ---
// const LayerPanel = ({ layers, onToggleVisibility, onDeleteLayer, onMoveLayer, onDuplicateLayer }) => {
//   return (
//     <div className="layer-panel">
//       <div className="layer-panel-header">
//         <span className="panel-title">LAYER STACK</span>
//         <span className="layer-count">{layers.length} layers</span>
//       </div>
//       <div className="layer-list">
//         {layers.length === 0 ? (
//           <div className="empty-layers">No layers yet. Apply filters to create layers.</div>
//         ) : (
//           layers.map((layer, index) => (
//             <div 
//               key={layer.id} 
//               className={`layer-item ${!layer.visible ? 'layer-hidden' : ''}`}
//             >
//               <div className="layer-visibility" onClick={() => onToggleVisibility(layer.id)}>
//                 {layer.visible ? '👁️' : '👁️‍🗨️'}
//               </div>
              
//               <div className="layer-info">
//                 <div className="layer-name">{layer.name}</div>
//                 <div className="layer-type">{layer.type}</div>
//                 {layer.type === 'color' && (
//                   <div className="layer-preview-color" style={{ backgroundColor: layer.color }}></div>
//                 )}
//                 {layer.type === 'gradient' && (
//                   <div className="layer-preview-gradient" style={{ background: layer.gradient }}></div>
//                 )}
//               </div>

//               <div className="layer-controls">
//                 <button 
//                   className="layer-btn" 
//                   onClick={() => onMoveLayer(index, 'up')}
//                   disabled={index === layers.length - 1}
//                   title="Move Up"
//                 >
//                   ▲
//                 </button>
//                 <button 
//                   className="layer-btn" 
//                   onClick={() => onMoveLayer(index, 'down')}
//                   disabled={index === 0}
//                   title="Move Down"
//                 >
//                   ▼
//                 </button>
//                 <button 
//                   className="layer-btn layer-btn-duplicate" 
//                   onClick={() => onDuplicateLayer(layer.id)}
//                   title="Duplicate"
//                 >
//                   ⧉
//                 </button>
//                 <button 
//                   className="layer-btn layer-btn-delete" 
//                   onClick={() => onDeleteLayer(layer.id)}
//                   title="Delete"
//                 >
//                   ×
//                 </button>
//               </div>
//             </div>
//           ))
//         )}
//       </div>
//     </div>
//   );
// };

// // --- Color/Gradient Layer Creator ---
// const LayerCreator = ({ onCreateLayer }) => {
//   const [showColorPicker, setShowColorPicker] = useState(false);
//   const [showGradientPicker, setShowGradientPicker] = useState(false);
//   const [solidColor, setSolidColor] = useState('#ff0000');
//   const [gradientStart, setGradientStart] = useState('#ff0000');
//   const [gradientEnd, setGradientEnd] = useState('#0000ff');
//   const [gradientAngle, setGradientAngle] = useState(90);

//   const handleCreateColor = () => {
//     onCreateLayer({
//       type: 'color',
//       color: solidColor,
//       name: `Color Layer (${solidColor})`
//     });
//     setShowColorPicker(false);
//   };

//   const handleCreateGradient = () => {
//     const gradient = `linear-gradient(${gradientAngle}deg, ${gradientStart}, ${gradientEnd})`;
//     onCreateLayer({
//       type: 'gradient',
//       gradient: gradient,
//       gradientStart,
//       gradientEnd,
//       gradientAngle,
//       name: `Gradient Layer (${gradientAngle}°)`
//     });
//     setShowGradientPicker(false);
//   };

//   return (
//     <div className="layer-creator">
//       <div className="creator-buttons">
//         <button 
//           className="btn-secondary" 
//           onClick={() => setShowColorPicker(!showColorPicker)}
//         >
//           + Color Layer
//         </button>
//         <button 
//           className="btn-secondary" 
//           onClick={() => setShowGradientPicker(!showGradientPicker)}
//         >
//           + Gradient Layer
//         </button>
//       </div>

//       {showColorPicker && (
//         <div className="picker-popup">
//           <div className="picker-header">Create Color Layer</div>
//           <input 
//             type="color" 
//             value={solidColor} 
//             onChange={(e) => setSolidColor(e.target.value)}
//             className="color-picker-input"
//           />
//           <input 
//             type="text" 
//             value={solidColor} 
//             onChange={(e) => setSolidColor(e.target.value)}
//             className="color-text-input"
//           />
//           <div className="picker-actions">
//             <button onClick={handleCreateColor}>Create</button>
//             <button className="btn-secondary" onClick={() => setShowColorPicker(false)}>Cancel</button>
//           </div>
//         </div>
//       )}

//       {showGradientPicker && (
//         <div className="picker-popup">
//           <div className="picker-header">Create Gradient Layer</div>
//           <div className="gradient-controls">
//             <div className="gradient-row">
//               <span>Start:</span>
//               <input 
//                 type="color" 
//                 value={gradientStart} 
//                 onChange={(e) => setGradientStart(e.target.value)}
//               />
//               <input 
//                 type="text" 
//                 value={gradientStart} 
//                 onChange={(e) => setGradientStart(e.target.value)}
//                 className="color-text-input"
//               />
//             </div>
//             <div className="gradient-row">
//               <span>End:</span>
//               <input 
//                 type="color" 
//                 value={gradientEnd} 
//                 onChange={(e) => setGradientEnd(e.target.value)}
//               />
//               <input 
//                 type="text" 
//                 value={gradientEnd} 
//                 onChange={(e) => setGradientEnd(e.target.value)}
//                 className="color-text-input"
//               />
//             </div>
//             <div className="gradient-row">
//               <span>Angle: {gradientAngle}°</span>
//               <input 
//                 type="range" 
//                 min="0" 
//                 max="360" 
//                 value={gradientAngle} 
//                 onChange={(e) => setGradientAngle(Number(e.target.value))}
//               />
//             </div>
//             <div 
//               className="gradient-preview" 
//               style={{ background: `linear-gradient(${gradientAngle}deg, ${gradientStart}, ${gradientEnd})` }}
//             ></div>
//           </div>
//           <div className="picker-actions">
//             <button onClick={handleCreateGradient}>Create</button>
//             <button className="btn-secondary" onClick={() => setShowGradientPicker(false)}>Cancel</button>
//           </div>
//         </div>
//       )}
//     </div>
//   );
// };

// export default function App() {
//   const [originalUrl, setOriginalUrl] = useState(null);
//   const [previewUrl, setPreviewUrl] = useState(null);
//   const [layerViewUrl, setLayerViewUrl] = useState(null);
//   const [originalBlob, setOriginalBlob] = useState(null);
//   const [sourceLinear, setSourceLinear] = useState(null);
//   const [resultLinear, setResultLinear] = useState(null);
//   const [layerLinear, setLayerLinear] = useState(null);
//   const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
//   const [resultDimensions, setResultDimensions] = useState({ width: 0, height: 0 });
//   const [layerDimensions, setLayerDimensions] = useState({ width: 0, height: 0 });
//   const [loading, setLoading] = useState(false);
//   const [mousePos, setMousePos] = useState(null);
//   const [lockedPos, setLockedPos] = useState(null);
//   const [isSquare, setIsSquare] = useState(null);

//   const [brightness, setBrightness] = useState(0);
//   const [contrast, setContrast] = useState(0);
//   const [blurIntensity, setBlurIntensity] = useState(0);
//   const [sharpenIntensity, setSharpenIntensity] = useState(0);
//   const [rotateAngle, setRotateAngle] = useState(0);
//   const [zoom, setZoom] = useState(1);
//   const [isGrayscaleMode, setIsGrayscaleMode] = useState(false);
  
//   const [cropData, setCropData] = useState({ x: 0, y: 0, w: 0, h: 0 });
//   const [isDragging, setIsDragging] = useState(false);
//   const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  
//   // Layer system state
//   const [layers, setLayers] = useState([]);
//   const [nextLayerId, setNextLayerId] = useState(1);
  
//   const fileInputRef = useRef(null);

//   const handleUpload = (e) => {
//     const file = e.target.files[0];
//     if (!file) return;
//     setOriginalBlob(file);
//     const url = URL.createObjectURL(file);
//     setOriginalUrl(url);

//     const img = new Image();
//     img.onload = async () => {
//       setDimensions({ width: img.width, height: img.height });
//       setResultDimensions({ width: img.width, height: img.height });
//       setLayerDimensions({ width: img.width, height: img.height });
//       setCropData({ x: 0, y: 0, w: 0, h: 0 });
      
//       const fd = new FormData();
//       fd.append("image", file);
//       fd.append("grayscale", false);
//       fd.append("level", 0);

//       try {
//         const res = await api.post("/brightness", fd);
//         setSourceLinear(res.data.linear);
//         setResultLinear(res.data.linear);
//         setLayerLinear(res.data.linear);
//         checkIfSquare(file);
//       } catch (err) { console.error(err); }
//     };
//     img.src = url;
//     setPreviewUrl(null);
//     setLayerViewUrl(null);
//     setLockedPos(null);
//     setLayers([]);
//     resetToolStates();
//   };

//   const handleToggleGrayscale = async () => {
//     const newMode = !isGrayscaleMode;
//     setIsGrayscaleMode(newMode);
    
//     if (originalBlob) {
//       setLoading(true);
//       const fd = new FormData();
//       fd.append("image", originalBlob);
//       fd.append("grayscale", newMode);
//       fd.append("level", 0); 

//       try {
//         const res = await api.post("/brightness", fd);
//         setSourceLinear(res.data.linear);
//         processImage('brightness', { level: brightness }); 
//       } catch (err) { console.error(err); } 
//       finally { setLoading(false); }
//     }
//   };

//   const checkIfSquare = async (file) => {
//     const fd = new FormData();
//     fd.append("image", file);
//     try {
//       const res = await api.post("/is-square", fd);
//       setIsSquare(res.data);
//     } catch (err) { console.error(err); }
//   };

//   const resetToolStates = () => {
//     setBrightness(0); setContrast(0); setBlurIntensity(0);
//     setSharpenIntensity(0); setRotateAngle(0); setZoom(1);
//     setIsGrayscaleMode(false); setCropData({ x: 0, y: 0, w: 0, h: 0 });
//   };

//   const handleResetAll = () => {
//     if (!originalBlob) return;
//     resetToolStates();
//     setPreviewUrl(null);
//     setResultLinear(null);
//     setLayerViewUrl(null);
//     setLayerLinear(null);
//     setLayers([]);
//     setLockedPos(null);
//     const img = new Image();
//     img.onload = () => {
//         setDimensions({ width: img.width, height: img.height });
//         setResultDimensions({ width: img.width, height: img.height });
//         setLayerDimensions({ width: img.width, height: img.height });
//     };
//     img.src = originalUrl;
//   };

//   const processImage = async (endpoint, params = {}) => {
//     if (!originalBlob) return;

//     if (!isGrayscaleMode && zoom === 1 && (endpoint === 'blur' || endpoint === 'sharpen') && params.intensity === 0) {
//         setPreviewUrl(null);
//         setResultLinear(null);
//         setResultDimensions({ width: dimensions.width, height: dimensions.height });
//         return;
//     }

//     setLoading(true);
//     const fd = new FormData();
//     fd.append("image", originalBlob);
//     fd.append("grayscale", isGrayscaleMode);
    
//     if (endpoint !== 'zoom' && zoom !== 1) {
//         fd.append("scale", zoom);
//     }

//     Object.entries(params).forEach(([key, val]) => fd.append(key, val));

//     try {
//       const res = await api.post(`${endpoint}`, fd);
//       setPreviewUrl(`data:image/png;base64,${res.data.image}`);
//       setResultLinear(res.data.linear);
      
//       setResultDimensions({ 
//           width: res.data.width, 
//           height: res.data.height 
//       });

//       if (endpoint === 'crop') {
//           setDimensions({ width: res.data.width, height: res.data.height });
//       }
//     } catch (err) { 
//       console.error("Filter application failed:", err); 
//     } finally { 
//       setLoading(false); 
//     }
//   };

//   // Add filter as layer
//   const handleAddFilterAsLayer = async (filterType, params) => {
//     if (!originalBlob) return;
    
//     setLoading(true);
//     const fd = new FormData();
//     fd.append("image", originalBlob);
//     fd.append("grayscale", isGrayscaleMode);
//     Object.entries(params).forEach(([key, val]) => fd.append(key, val));

//     try {
//       const res = await api.post(`/${filterType}`, fd);
      
//       const newLayer = {
//         id: nextLayerId,
//         name: `${filterType.charAt(0).toUpperCase() + filterType.slice(1)} Layer`,
//         type: 'filter',
//         filterType: filterType,
//         params: params,
//         visible: true,
//         imageData: `data:image/png;base64,${res.data.image}`,
//         linear: res.data.linear,
//         width: res.data.width,
//         height: res.data.height
//       };
      
//       setLayers(prev => [...prev, newLayer]);
//       setNextLayerId(prev => prev + 1);
//       await updateLayerComposite([...layers, newLayer]);
//     } catch (err) { 
//       console.error("Failed to add filter layer:", err); 
//     } finally { 
//       setLoading(false); 
//     }
//   };

//   // Create color/gradient layer
//   const handleCreateLayer = (layerData) => {
//     const newLayer = {
//       id: nextLayerId,
//       ...layerData,
//       visible: true
//     };
    
//     setLayers(prev => [...prev, newLayer]);
//     setNextLayerId(prev => prev + 1);
//     updateLayerComposite([...layers, newLayer]);
//   };

//   // Update layer composite by sending to backend
//   const updateLayerComposite = async (currentLayers) => {
//     if (!originalBlob || currentLayers.length === 0) {
//       setLayerViewUrl(null);
//       setLayerLinear(null);
//       return;
//     }

//     setLoading(true);
//     try {
//       const visibleLayers = currentLayers.filter(l => l.visible);
//       const fd = new FormData();
//       fd.append("image", originalBlob);
//       fd.append("layers", JSON.stringify(visibleLayers));

//       const res = await api.post("/composite-layers", fd);
//       setLayerViewUrl(`data:image/png;base64,${res.data.image}`);
//       setLayerLinear(res.data.linear);
//       setLayerDimensions({
//         width: res.data.width,
//         height: res.data.height
//       });
//     } catch (err) {
//       console.error("Composite failed:", err);
//     } finally {
//       setLoading(false);
//     }
//   };

//   // Layer operations
//   const handleToggleVisibility = (layerId) => {
//     const newLayers = layers.map(layer => 
//       layer.id === layerId ? { ...layer, visible: !layer.visible } : layer
//     );
//     setLayers(newLayers);
//     updateLayerComposite(newLayers);
//   };

//   const handleDeleteLayer = (layerId) => {
//     const newLayers = layers.filter(layer => layer.id !== layerId);
//     setLayers(newLayers);
//     updateLayerComposite(newLayers);
//   };

//   const handleMoveLayer = (index, direction) => {
//     const newLayers = [...layers];
//     if (direction === 'up' && index < layers.length - 1) {
//       [newLayers[index], newLayers[index + 1]] = [newLayers[index + 1], newLayers[index]];
//     } else if (direction === 'down' && index > 0) {
//       [newLayers[index], newLayers[index - 1]] = [newLayers[index - 1], newLayers[index]];
//     }
//     setLayers(newLayers);
//     updateLayerComposite(newLayers);
//   };

//   const handleDuplicateLayer = (layerId) => {
//     const layer = layers.find(l => l.id === layerId);
//     if (layer) {
//       const newLayer = {
//         ...layer,
//         id: nextLayerId,
//         name: `${layer.name} (Copy)`
//       };
//       setLayers(prev => [...prev, newLayer]);
//       setNextLayerId(prev => prev + 1);
//       updateLayerComposite([...layers, newLayer]);
//     }
//   };

//   const handleMouseDown = (e) => {
//     const rect = e.currentTarget.getBoundingClientRect();
//     const x = Math.floor(e.clientX - rect.left);
//     const y = Math.floor(e.clientY - rect.top);
//     setStartPos({ x, y });
//     setIsDragging(true);
//   };

//   const handleMouseMoveViewport = (e, isResult) => {
//     const rect = e.currentTarget.getBoundingClientRect();
//     const x = Math.floor(e.clientX - rect.left);
//     const y = Math.floor(e.clientY - rect.top);
//     setMousePos({ x, y });

//     if (isDragging && !isResult) {
//       const left = Math.max(0, Math.min(startPos.x, x));
//       const top = Math.max(0, Math.min(startPos.y, y));
//       const width = Math.min(dimensions.width - left, Math.abs(x - startPos.x));
//       const height = Math.min(dimensions.height - top, Math.abs(y - startPos.y));
//       setCropData({ x: left, y: top, w: width, h: height });
//     }
//   };

//   const toggleLock = () => {
//     if (lockedPos) setLockedPos(null);
//     else setLockedPos({ ...mousePos });
//   };

//   return (
//     <div className="pixel-app">
//       <header className="brand-header">
//         <div className="logo">PIXEL<span>GREY</span>.v2</div>
//         <div className="header-controls">
//             {isSquare !== null && (
//                 <div className={`square-badge ${isSquare ? 'is-square' : 'not-square'}`}>
//                     {isSquare ? "■ SQUARE_MATCH" : "▭ ASYMMETRIC"}
//                 </div>
//             )}
//             <button className={`btn-secondary ${isGrayscaleMode ? 'active' : ''}`} onClick={handleToggleGrayscale}>
//                 {isGrayscaleMode ? "MODE: GRAYSCALE" : "MODE: COLOR"}
//             </button>
//         </div>
//         <div className="status-indicator" style={{color: loading ? 'orange' : '#10b981'}}>
//             {loading ? "BUSY" : "SYSTEM_READY"}
//         </div>
//       </header>

//       <nav className="toolbar">
//         <div className="tool-unit">
//           <span className="tool-label">Source</span>
//           <button onClick={() => fileInputRef.current.click()}>Upload</button>
//           <input type="file" ref={fileInputRef} onChange={handleUpload} hidden />
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Adjustments</span>
//           <div className="slider-group">
//             <div className="slider-item">
//                 <span>Bri({brightness})</span>
//                 <input type="range" min="-100" max="100" value={brightness} onChange={e => setBrightness(+e.target.value)} onMouseUp={() => processImage('brightness', { level: brightness })} />
//             </div>
//             <div className="slider-item">
//                 <span>Con({contrast})</span>
//                 <input type="range" min="-100" max="100" value={contrast} onChange={e => setContrast(+e.target.value)} onMouseUp={() => processImage('contrast', { level: contrast })} />
//             </div>
//           </div>
//           <button 
//             className="btn-primary" 
//             style={{marginTop: '8px', width: '100%'}}
//             onClick={() => {
//               if (brightness !== 0) handleAddFilterAsLayer('brightness', { level: brightness });
//               else if (contrast !== 0) handleAddFilterAsLayer('contrast', { level: contrast });
//             }}
//           >
//             Add as Layer
//           </button>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Filters</span>
//           <div className="slider-group">
//             <div className="slider-item">
//                 <span>Blur({blurIntensity})</span>
//                 <input type="range" min="0" max="20" value={blurIntensity} onChange={e => setBlurIntensity(+e.target.value)} onMouseUp={() => processImage('blur', { intensity: blurIntensity })} />
//             </div>
//             <div className="slider-item">
//                 <span>Sharp({sharpenIntensity})</span>
//                 <input type="range" min="0" max="20" value={sharpenIntensity} onChange={e => setSharpenIntensity(+e.target.value)} onMouseUp={() => processImage('sharpen', { intensity: sharpenIntensity })} />
//             </div>
//           </div>
//           <button 
//             className="btn-primary" 
//             style={{marginTop: '8px', width: '100%'}}
//             onClick={() => {
//               if (blurIntensity !== 0) handleAddFilterAsLayer('blur', { intensity: blurIntensity });
//               else if (sharpenIntensity !== 0) handleAddFilterAsLayer('sharpen', { intensity: sharpenIntensity });
//             }}
//           >
//             Add as Layer
//           </button>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Transform</span>
//           <div className="input-group">
//             <input type="number" className="number-input" value={rotateAngle} onChange={(e) => setRotateAngle(e.target.value)} />
//             <button onClick={() => processImage('rotate', { angle: rotateAngle })}>Rotate</button>
//           </div>
//           <div className="btn-group" style={{marginTop:'5px'}}>
//             <button onClick={() => processImage('flip/horizontal')}>Flip H</button>
//             <button onClick={() => processImage('flip/vertical')}>Flip V</button>
//           </div>
//         </div>

//         <div className="tool-unit">
//           <span className="tool-label">Zoom Result ({zoom}x)</span>
//           <input type="range" min="1" max="4" step="1" value={zoom} onChange={e => setZoom(+e.target.value)} onMouseUp={() => processImage('zoom', { scale: zoom })} />
//           <div className="btn-group" style={{marginTop:'5px'}}>
//             <button className="btn-secondary" onClick={() => setLockedPos(null)}>Unlock</button>
//             <button className="btn-secondary" onClick={handleResetAll}>Reset All</button>
//           </div>
//         </div>
//       </nav>

//       <main className="workspace-with-layers">
//         <section className="panel-left">
//           <MatrixOverlay linearData={sourceLinear} width={dimensions.width} height={dimensions.height} pos={lockedPos || mousePos} title="SOURCE" isLocked={!!lockedPos} />
//           <MatrixOverlay 
//             linearData={resultLinear || sourceLinear} 
//             width={resultDimensions.width} 
//             height={resultDimensions.height} 
//             pos={
//               (lockedPos || mousePos) 
//                 ? { 
//                     x: (lockedPos ? lockedPos.x : mousePos.x) * (resultDimensions.width / dimensions.width), 
//                     y: (lockedPos ? lockedPos.y : mousePos.y) * (resultDimensions.height / dimensions.height) 
//                   } 
//                 : null
//             } 
//             title="RESULT" 
//             isLocked={!!lockedPos} 
//           />
//           <MatrixOverlay 
//             linearData={layerLinear || sourceLinear} 
//             width={layerDimensions.width} 
//             height={layerDimensions.height} 
//             pos={
//               (lockedPos || mousePos) 
//                 ? { 
//                     x: (lockedPos ? lockedPos.x : mousePos.x) * (layerDimensions.width / dimensions.width), 
//                     y: (lockedPos ? lockedPos.y : mousePos.y) * (layerDimensions.height / dimensions.height) 
//                   } 
//                 : null
//             } 
//             title="LAYER VIEW" 
//             isLocked={!!lockedPos} 
//           />
//         </section>

//         <section className="preview-panel-three">
//           <div className="comparison-view-three">
//             <div className="viewport-third">
//               <span className="view-tag">ORIGINAL</span>
//               <div className="viewport-container" 
//                    onClick={toggleLock} 
//                    onMouseDown={handleMouseDown}
//                    onMouseMove={(e) => handleMouseMoveViewport(e, false)} 
//                    onMouseUp={() => setIsDragging(false)}>
//                 {originalUrl && (
//                   <img src={originalUrl} className="preview-img" draggable="false" alt="source" 
//                        style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
//                 )}
//                 {isDragging && <div className="crop-selector-visual" style={{position:'absolute', border:'2px dashed #6366f1', backgroundColor:'rgba(99,102,241,0.2)', left:cropData.x, top:cropData.y, width:cropData.w, height:cropData.h, pointerEvents:'none'}} />}
//               </div>
//             </div>
            
//             <div className="viewport-third">
//               <span className="view-tag">RESULT</span>
//               <div className="viewport-container" onClick={toggleLock} onMouseMove={(e) => handleMouseMoveViewport(e, true)}>
//                 {previewUrl ? (
//                   <img src={previewUrl} className="preview-img" draggable="false" alt="result" style={{ imageRendering: 'pixelated' }} />
//                 ) : (
//                   originalUrl && <img src={originalUrl} className="preview-img" draggable="false" alt="source-copy" 
//                                       style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
//                 )}
//               </div>
//             </div>

//             <div className="viewport-third">
//               <span className="view-tag">LAYER VIEW</span>
//               <div className="viewport-container" onClick={toggleLock} onMouseMove={(e) => handleMouseMoveViewport(e, true)}>
//                 {layerViewUrl ? (
//                   <img src={layerViewUrl} className="preview-img" draggable="false" alt="layers" style={{ imageRendering: 'pixelated' }} />
//                 ) : (
//                   originalUrl && <img src={originalUrl} className="preview-img" draggable="false" alt="source-copy" 
//                                       style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
//                 )}
//               </div>
//             </div>
//           </div>
//         </section>

//         <section className="panel-right">
//           <LayerCreator onCreateLayer={handleCreateLayer} />
//           <LayerPanel 
//             layers={layers}
//             onToggleVisibility={handleToggleVisibility}
//             onDeleteLayer={handleDeleteLayer}
//             onMoveLayer={handleMoveLayer}
//             onDuplicateLayer={handleDuplicateLayer}
//           />
//         </section>
//       </main>
//     </div>
//   );
// }
import React, { useState, useRef, useEffect } from "react";
import api from "./api/axios";
import "./App.css";

// --- Matrix Overlay Component ---
const MatrixOverlay = ({ linearData, width, height, pos, title, isLocked }) => {
  if (!linearData || !pos || width === 0) {
    return <div className="matrix-viewer empty">HOVER OR CLICK {title}</div>;
  }

  const radius = 2;
  const px = Math.floor(pos.x); 
  const py = Math.floor(pos.y);
  const grid = [];

  for (let dy = -radius; dy <= radius; dy++) {
    const row = [];
    for (let dx = -radius; dx <= radius; dx++) {
      const x = px + dx; 
      const y = py + dy;

      if (x >= 0 && x < width && y >= 0 && y < height) {
        const val = linearData[y * width + x];
        if (typeof val === 'object' && val !== null) {
          row.push(val);
        } else if (typeof val === 'number') {
          row.push({
            r: (val >> 16) & 0xFF,
            g: (val >> 8) & 0xFF,
            b: val & 0xFF
          });
        } else {
          row.push(null);
        }
      } else {
        row.push(null);
      }
    }
    grid.push(row);
  }

  return (
    <div className={`matrix-viewer ${isLocked ? 'locked' : ''}`}>
      <div className="matrix-header">
        <span className="matrix-title">{title}</span>
        <span className="matrix-coords">[{px}, {py}] {isLocked && "🔒"}</span>
      </div>
      <div className="matrix-grid">
        {grid.map((row, i) => (
          <div key={i} className="matrix-row">
            {row.map((pixel, j) => (
              <div key={j} className="matrix-cell rgb-cell" style={{ 
                backgroundColor: pixel ? `rgb(${pixel.r},${pixel.g},${pixel.b})` : '#111',
                color: (pixel && (pixel.r + pixel.g + pixel.b)/3 > 128) ? '#000' : '#fff',
                border: (i===2 && j===2) ? '2px solid #6366f1' : '1px solid #333'
              }}>
                {pixel ? (
                  <div className="rgb-values">
                    <span>{pixel.r}</span>
                    <span>{pixel.g}</span>
                    <span>{pixel.b}</span>
                  </div>
                ) : '--'}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

// --- Layer Panel Component ---
const LayerPanel = ({ layers, onToggleVisibility, onDeleteLayer, onMoveLayer, onDuplicateLayer, onOpacityChange }) => {
  return (
    <div className="layer-panel">
      <div className="layer-panel-header">
        <span className="panel-title">LAYER STACK</span>
        <span className="layer-count">{layers.length} layers</span>
      </div>
      <div className="layer-list">
        {layers.length === 0 ? (
          <div className="empty-layers">No layers yet. Apply filters to create layers.</div>
        ) : (
          layers.map((layer, index) => (
            <div 
              key={layer.id} 
              className={`layer-item ${!layer.visible ? 'layer-hidden' : ''}`}
            >
              <div className="layer-visibility" onClick={() => onToggleVisibility(layer.id)}>
                {layer.visible ? '👁️' : '👁️‍🗨️'}
              </div>
              
              <div className="layer-info">
                <div className="layer-name">{layer.name}</div>
                <div className="layer-type">{layer.type}</div>
                {layer.type === 'color' && (
                  <div className="layer-preview-color" style={{ backgroundColor: layer.color }}></div>
                )}
                {layer.type === 'gradient' && (
                  <div className="layer-preview-gradient" style={{ background: layer.gradient }}></div>
                )}
                
                {/* Opacity slider for color, gradient, and image layers */}
                {(layer.type === 'color' || layer.type === 'gradient' || layer.type === 'image') && (
                  <div className="layer-opacity-control">
                    <span className="opacity-label">Opacity: {Math.round(layer.opacity * 100)}%</span>
                    <input 
                      type="range" 
                      min="0" 
                      max="100" 
                      value={Math.round(layer.opacity * 100)} 
                      onChange={(e) => onOpacityChange(layer.id, parseFloat(e.target.value) / 100)}
                      className="opacity-slider"
                    />
                  </div>
                )}
              </div>

              <div className="layer-controls">
                <button 
                  className="layer-btn" 
                  onClick={() => onMoveLayer(index, 'up')}
                  disabled={index === layers.length - 1}
                  title="Move Up"
                >
                  ▲
                </button>
                <button 
                  className="layer-btn" 
                  onClick={() => onMoveLayer(index, 'down')}
                  disabled={index === 0}
                  title="Move Down"
                >
                  ▼
                </button>
                <button 
                  className="layer-btn layer-btn-duplicate" 
                  onClick={() => onDuplicateLayer(layer.id)}
                  title="Duplicate"
                >
                  ⧉
                </button>
                <button 
                  className="layer-btn layer-btn-delete" 
                  onClick={() => onDeleteLayer(layer.id)}
                  title="Delete"
                >
                  ×
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

// --- Color/Gradient Layer Creator ---
const LayerCreator = ({ onCreateLayer, onAddImageLayer }) => {
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [showGradientPicker, setShowGradientPicker] = useState(false);
  const [solidColor, setSolidColor] = useState('#ff0000');
  const [gradientStart, setGradientStart] = useState('#ff0000');
  const [gradientEnd, setGradientEnd] = useState('#0000ff');
  const [gradientAngle, setGradientAngle] = useState(90);
  const imageInputRef = useRef(null);

  const handleCreateColor = () => {
    onCreateLayer({
      type: 'color',
      color: solidColor,
      opacity: 1.0,
      name: `Color Layer (${solidColor})`
    });
    setShowColorPicker(false);
  };

  const handleCreateGradient = () => {
    const gradient = `linear-gradient(${gradientAngle}deg, ${gradientStart}, ${gradientEnd})`;
    onCreateLayer({
      type: 'gradient',
      gradient: gradient,
      gradientStart,
      gradientEnd,
      gradientAngle,
      opacity: 1.0,
      name: `Gradient Layer (${gradientAngle}°)`
    });
    setShowGradientPicker(false);
  };

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = (event) => {
      onAddImageLayer(event.target.result, file);
    };
    reader.readAsDataURL(file);
  };

  return (
    <div className="layer-creator">
      <div className="creator-buttons">
        <button 
          className="btn-secondary" 
          onClick={() => setShowColorPicker(!showColorPicker)}
        >
          + Color Layer
        </button>
        <button 
          className="btn-secondary" 
          onClick={() => setShowGradientPicker(!showGradientPicker)}
        >
          + Gradient Layer
        </button>
        <button 
          className="btn-secondary" 
          onClick={() => imageInputRef.current.click()}
        >
          + Mix Image
        </button>
        <input 
          type="file" 
          ref={imageInputRef} 
          onChange={handleImageUpload} 
          accept="image/*"
          hidden 
        />
      </div>

      {showColorPicker && (
        <div className="picker-popup">
          <div className="picker-header">Create Color Layer</div>
          <input 
            type="color" 
            value={solidColor} 
            onChange={(e) => setSolidColor(e.target.value)}
            className="color-picker-input"
          />
          <input 
            type="text" 
            value={solidColor} 
            onChange={(e) => setSolidColor(e.target.value)}
            className="color-text-input"
          />
          <div className="picker-actions">
            <button onClick={handleCreateColor}>Create</button>
            <button className="btn-secondary" onClick={() => setShowColorPicker(false)}>Cancel</button>
          </div>
        </div>
      )}

      {showGradientPicker && (
        <div className="picker-popup">
          <div className="picker-header">Create Gradient Layer</div>
          <div className="gradient-controls">
            <div className="gradient-row">
              <span>Start:</span>
              <input 
                type="color" 
                value={gradientStart} 
                onChange={(e) => setGradientStart(e.target.value)}
              />
              <input 
                type="text" 
                value={gradientStart} 
                onChange={(e) => setGradientStart(e.target.value)}
                className="color-text-input"
              />
            </div>
            <div className="gradient-row">
              <span>End:</span>
              <input 
                type="color" 
                value={gradientEnd} 
                onChange={(e) => setGradientEnd(e.target.value)}
              />
              <input 
                type="text" 
                value={gradientEnd} 
                onChange={(e) => setGradientEnd(e.target.value)}
                className="color-text-input"
              />
            </div>
            <div className="gradient-row">
              <span>Angle: {gradientAngle}°</span>
              <input 
                type="range" 
                min="0" 
                max="360" 
                value={gradientAngle} 
                onChange={(e) => setGradientAngle(Number(e.target.value))}
              />
            </div>
            <div 
              className="gradient-preview" 
              style={{ background: `linear-gradient(${gradientAngle}deg, ${gradientStart}, ${gradientEnd})` }}
            ></div>
          </div>
          <div className="picker-actions">
            <button onClick={handleCreateGradient}>Create</button>
            <button className="btn-secondary" onClick={() => setShowGradientPicker(false)}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default function App() {
  const [originalUrl, setOriginalUrl] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [layerViewUrl, setLayerViewUrl] = useState(null);
  const [originalBlob, setOriginalBlob] = useState(null);
  const [sourceLinear, setSourceLinear] = useState(null);
  const [resultLinear, setResultLinear] = useState(null);
  const [layerLinear, setLayerLinear] = useState(null);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [resultDimensions, setResultDimensions] = useState({ width: 0, height: 0 });
  const [layerDimensions, setLayerDimensions] = useState({ width: 0, height: 0 });
  const [loading, setLoading] = useState(false);
  const [mousePos, setMousePos] = useState(null);
  const [lockedPos, setLockedPos] = useState(null);
  const [isSquare, setIsSquare] = useState(null);

  const [brightness, setBrightness] = useState(0);
  const [contrast, setContrast] = useState(0);
  const [blurIntensity, setBlurIntensity] = useState(0);
  const [sharpenIntensity, setSharpenIntensity] = useState(0);
  const [rotateAngle, setRotateAngle] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [isGrayscaleMode, setIsGrayscaleMode] = useState(false);
  
  const [cropData, setCropData] = useState({ x: 0, y: 0, w: 0, h: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  const [isCropMode, setIsCropMode] = useState(false);
  
  // Layer system state
  const [layers, setLayers] = useState([]);
  const [nextLayerId, setNextLayerId] = useState(1);
  
  const fileInputRef = useRef(null);

  const handleUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setOriginalBlob(file);
    const url = URL.createObjectURL(file);
    setOriginalUrl(url);

    const img = new Image();
    img.onload = async () => {
      setDimensions({ width: img.width, height: img.height });
      setResultDimensions({ width: img.width, height: img.height });
      setLayerDimensions({ width: img.width, height: img.height });
      setCropData({ x: 0, y: 0, w: 0, h: 0 });
      
      const fd = new FormData();
      fd.append("image", file);
      fd.append("grayscale", false);
      fd.append("level", 0);

      try {
        const res = await api.post("/brightness", fd);
        setSourceLinear(res.data.linear);
        setResultLinear(res.data.linear);
        setLayerLinear(res.data.linear);
        checkIfSquare(file);
      } catch (err) { console.error(err); }
    };
    img.src = url;
    setPreviewUrl(null);
    setLayerViewUrl(null);
    setLockedPos(null);
    setLayers([]);
    resetToolStates();
  };

  const handleToggleGrayscale = async () => {
    const newMode = !isGrayscaleMode;
    setIsGrayscaleMode(newMode);
    
    if (originalBlob) {
      setLoading(true);
      const fd = new FormData();
      fd.append("image", originalBlob);
      fd.append("grayscale", newMode);
      fd.append("level", 0); 

      try {
        const res = await api.post("/brightness", fd);
        setSourceLinear(res.data.linear);
        
        // Reset preview if blur is at 0
        if (blurIntensity === 0 && brightness === 0 && contrast === 0 && sharpenIntensity === 0 && zoom === 1) {
          setPreviewUrl(null);
          setResultLinear(res.data.linear);
        } else {
          processImage('brightness', { level: brightness });
        }
      } catch (err) { console.error(err); } 
      finally { setLoading(false); }
    }
  };

  const checkIfSquare = async (file) => {
    const fd = new FormData();
    fd.append("image", file);
    try {
      const res = await api.post("/is-square", fd);
      setIsSquare(res.data);
    } catch (err) { console.error(err); }
  };

  const resetToolStates = () => {
    setBrightness(0); setContrast(0); setBlurIntensity(0);
    setSharpenIntensity(0); setRotateAngle(0); setZoom(1);
    setIsGrayscaleMode(false); setCropData({ x: 0, y: 0, w: 0, h: 0 });
    setIsCropMode(false);
  };

  const handleResetAll = () => {
    if (!originalBlob) return;
    resetToolStates();
    setPreviewUrl(null);
    setResultLinear(null);
    setLayerViewUrl(null);
    setLayerLinear(null);
    setLayers([]);
    setLockedPos(null);
    const img = new Image();
    img.onload = () => {
        setDimensions({ width: img.width, height: img.height });
        setResultDimensions({ width: img.width, height: img.height });
        setLayerDimensions({ width: img.width, height: img.height });
    };
    img.src = originalUrl;
  };

  const processImage = async (endpoint, params = {}) => {
    if (!originalBlob) return;

    // Reset preview if blur is at 0 in any mode
    if (!isGrayscaleMode && zoom === 1 && endpoint === 'blur' && params.intensity === 0) {
        setPreviewUrl(null);
        setResultLinear(sourceLinear);
        setResultDimensions({ width: dimensions.width, height: dimensions.height });
        return;
    }

    // Also reset if in grayscale mode and blur is 0
    if (isGrayscaleMode && zoom === 1 && endpoint === 'blur' && params.intensity === 0) {
        setPreviewUrl(null);
        setResultLinear(sourceLinear);
        setResultDimensions({ width: dimensions.width, height: dimensions.height });
        return;
    }

    setLoading(true);
    const fd = new FormData();
    fd.append("image", originalBlob);
    fd.append("grayscale", isGrayscaleMode);
    
    if (endpoint !== 'zoom' && zoom !== 1) {
        fd.append("scale", zoom);
    }

    Object.entries(params).forEach(([key, val]) => fd.append(key, val));

    try {
      const res = await api.post(`${endpoint}`, fd);
      setPreviewUrl(`data:image/png;base64,${res.data.image}`);
      setResultLinear(res.data.linear);
      
      setResultDimensions({ 
          width: res.data.width, 
          height: res.data.height 
      });

      if (endpoint === 'crop') {
          setDimensions({ width: res.data.width, height: res.data.height });
      }
    } catch (err) { 
      console.error("Filter application failed:", err); 
    } finally { 
      setLoading(false); 
    }
  };

  // Add filter as layer
  const handleAddFilterAsLayer = async (filterType, params) => {
    if (!originalBlob) return;
    
    setLoading(true);
    const fd = new FormData();
    fd.append("image", originalBlob);
    fd.append("grayscale", isGrayscaleMode);
    Object.entries(params).forEach(([key, val]) => fd.append(key, val));

    try {
      const res = await api.post(`/${filterType}`, fd);
      
      const newLayer = {
        id: nextLayerId,
        name: `${filterType.charAt(0).toUpperCase() + filterType.slice(1)} Layer`,
        type: 'filter',
        filterType: filterType,
        params: params,
        visible: true,
        opacity: 1.0,
        imageData: `data:image/png;base64,${res.data.image}`,
        linear: res.data.linear,
        width: res.data.width,
        height: res.data.height
      };
      
      setLayers(prev => [...prev, newLayer]);
      setNextLayerId(prev => prev + 1);
      await updateLayerComposite([...layers, newLayer]);
    } catch (err) { 
      console.error("Failed to add filter layer:", err); 
    } finally { 
      setLoading(false); 
    }
  };

  // Add grayscale as layer
  const handleAddGrayscaleLayer = async () => {
    if (!originalBlob) return;
    
    setLoading(true);
    const fd = new FormData();
    fd.append("image", originalBlob);

    try {
      const res = await api.post("/grayscale", fd);
      
      const newLayer = {
        id: nextLayerId,
        name: 'Grayscale Layer',
        type: 'filter',
        filterType: 'grayscale',
        params: {},
        visible: true,
        opacity: 1.0,
        imageData: `data:image/png;base64,${res.data.image}`,
        linear: res.data.linear,
        width: res.data.width,
        height: res.data.height
      };
      
      setLayers(prev => [...prev, newLayer]);
      setNextLayerId(prev => prev + 1);
      await updateLayerComposite([...layers, newLayer]);
    } catch (err) { 
      console.error("Failed to add grayscale layer:", err); 
    } finally { 
      setLoading(false); 
    }
  };

  // Create color/gradient layer
  const handleCreateLayer = (layerData) => {
    const newLayer = {
      id: nextLayerId,
      ...layerData,
      visible: true
    };
    
    setLayers(prev => [...prev, newLayer]);
    setNextLayerId(prev => prev + 1);
    updateLayerComposite([...layers, newLayer]);
  };

  // Add image layer for mixing
  const handleAddImageLayer = (imageDataUrl, file) => {
    const newLayer = {
      id: nextLayerId,
      name: `Image Layer (${file.name})`,
      type: 'image',
      visible: true,
      opacity: 0.5,
      imageData: imageDataUrl,
      fileName: file.name
    };
    
    setLayers(prev => [...prev, newLayer]);
    setNextLayerId(prev => prev + 1);
    updateLayerComposite([...layers, newLayer]);
  };

  // Update layer composite by sending to backend
  const updateLayerComposite = async (currentLayers) => {
    if (!originalBlob || currentLayers.length === 0) {
      setLayerViewUrl(null);
      setLayerLinear(null);
      return;
    }

    setLoading(true);
    try {
      const visibleLayers = currentLayers.filter(l => l.visible);
      const fd = new FormData();
      fd.append("image", originalBlob);
      fd.append("layers", JSON.stringify(visibleLayers));

      const res = await api.post("/composite-layers", fd);
      setLayerViewUrl(`data:image/png;base64,${res.data.image}`);
      setLayerLinear(res.data.linear);
      setLayerDimensions({
        width: res.data.width,
        height: res.data.height
      });
    } catch (err) {
      console.error("Composite failed:", err);
    } finally {
      setLoading(false);
    }
  };

  // Layer operations
  const handleToggleVisibility = (layerId) => {
    const newLayers = layers.map(layer => 
      layer.id === layerId ? { ...layer, visible: !layer.visible } : layer
    );
    setLayers(newLayers);
    updateLayerComposite(newLayers);
  };

  const handleDeleteLayer = (layerId) => {
    const newLayers = layers.filter(layer => layer.id !== layerId);
    setLayers(newLayers);
    updateLayerComposite(newLayers);
  };

  const handleMoveLayer = (index, direction) => {
    const newLayers = [...layers];
    if (direction === 'up' && index < layers.length - 1) {
      [newLayers[index], newLayers[index + 1]] = [newLayers[index + 1], newLayers[index]];
    } else if (direction === 'down' && index > 0) {
      [newLayers[index], newLayers[index - 1]] = [newLayers[index - 1], newLayers[index]];
    }
    setLayers(newLayers);
    updateLayerComposite(newLayers);
  };

  const handleDuplicateLayer = (layerId) => {
    const layer = layers.find(l => l.id === layerId);
    if (layer) {
      const newLayer = {
        ...layer,
        id: nextLayerId,
        name: `${layer.name} (Copy)`
      };
      setLayers(prev => [...prev, newLayer]);
      setNextLayerId(prev => prev + 1);
      updateLayerComposite([...layers, newLayer]);
    }
  };

  const handleOpacityChange = (layerId, newOpacity) => {
    const newLayers = layers.map(layer => 
      layer.id === layerId ? { ...layer, opacity: newOpacity } : layer
    );
    setLayers(newLayers);
    updateLayerComposite(newLayers);
  };

  // Submit final composite
  const handleSubmitFinal = async () => {
    if (!originalBlob || layers.length === 0) {
      alert("Please add layers before submitting!");
      return;
    }

    setLoading(true);
    try {
      const visibleLayers = layers.filter(l => l.visible);
      const fd = new FormData();
      fd.append("image", originalBlob);
      fd.append("layers", JSON.stringify(visibleLayers));

      const res = await api.post("/composite-layers", fd);
      
      // Create download link
      const base64Image = res.data.image;
      const link = document.createElement('a');
      link.href = `data:image/png;base64,${base64Image}`;
      link.download = 'pixelgrey-final-composite.png';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      alert("Final composite downloaded successfully!");
    } catch (err) {
      console.error("Failed to create final composite:", err);
      alert("Failed to create final composite. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // Crop functionality
  const handleApplyCrop = async () => {
    if (!originalBlob || cropData.w === 0 || cropData.h === 0) {
      alert("Please select a crop area first!");
      return;
    }

    setLoading(true);
    const fd = new FormData();
    fd.append("image", originalBlob);
    fd.append("x", cropData.x);
    fd.append("y", cropData.y);
    fd.append("w", cropData.w);
    fd.append("h", cropData.h);
    fd.append("grayscale", isGrayscaleMode);

    try {
      const res = await api.post("/crop", fd);
      
      // Update original image with cropped version
      const newBlob = await fetch(`data:image/png;base64,${res.data.image}`).then(r => r.blob());
      setOriginalBlob(newBlob);
      
      const newUrl = URL.createObjectURL(newBlob);
      setOriginalUrl(newUrl);
      
      setDimensions({ width: res.data.width, height: res.data.height });
      setResultDimensions({ width: res.data.width, height: res.data.height });
      setLayerDimensions({ width: res.data.width, height: res.data.height });
      
      setSourceLinear(res.data.linear);
      setResultLinear(res.data.linear);
      setLayerLinear(res.data.linear);
      
      setCropData({ x: 0, y: 0, w: 0, h: 0 });
      setIsCropMode(false);
      setPreviewUrl(null);
      
    } catch (err) {
      console.error("Crop failed:", err);
      alert("Crop operation failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleMouseDown = (e) => {
    if (!isCropMode) return;
    
    const rect = e.currentTarget.getBoundingClientRect();
    const x = Math.floor(e.clientX - rect.left);
    const y = Math.floor(e.clientY - rect.top);
    setStartPos({ x, y });
    setIsDragging(true);
  };

  const handleMouseMoveViewport = (e, isResult) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = Math.floor(e.clientX - rect.left);
    const y = Math.floor(e.clientY - rect.top);
    setMousePos({ x, y });

    if (isDragging && !isResult && isCropMode) {
      const left = Math.max(0, Math.min(startPos.x, x));
      const top = Math.max(0, Math.min(startPos.y, y));
      const width = Math.min(dimensions.width - left, Math.abs(x - startPos.x));
      const height = Math.min(dimensions.height - top, Math.abs(y - startPos.y));
      setCropData({ x: left, y: top, w: width, h: height });
    }
  };

  const toggleLock = () => {
    if (lockedPos) setLockedPos(null);
    else setLockedPos({ ...mousePos });
  };

  return (
    <div className="pixel-app">
      <header className="brand-header">
        <div className="logo">PIXEL<span>GREY</span>.v2</div>
        <div className="header-controls">
            {isSquare !== null && (
                <div className={`square-badge ${isSquare ? 'is-square' : 'not-square'}`}>
                    {isSquare ? "■ SQUARE_MATCH" : "▭ ASYMMETRIC"}
                </div>
            )}
            <button className={`btn-secondary ${isGrayscaleMode ? 'active' : ''}`} onClick={handleToggleGrayscale}>
                {isGrayscaleMode ? "MODE: GRAYSCALE" : "MODE: COLOR"}
            </button>
            <button 
              className="btn-primary submit-final-btn" 
              onClick={handleSubmitFinal}
              disabled={layers.length === 0}
            >
              🔽 DOWNLOAD FINAL
            </button>
        </div>
        <div className="status-indicator" style={{color: loading ? 'orange' : '#10b981'}}>
            {loading ? "BUSY" : "SYSTEM_READY"}
        </div>
      </header>

      <nav className="toolbar">
        <div className="tool-unit">
          <span className="tool-label">Source</span>
          <button onClick={() => fileInputRef.current.click()}>Upload</button>
          <input type="file" ref={fileInputRef} onChange={handleUpload} hidden />
        </div>

        <div className="tool-unit">
          <span className="tool-label">Crop Tool</span>
          <button 
            className={`btn-secondary ${isCropMode ? 'active' : ''}`}
            onClick={() => setIsCropMode(!isCropMode)}
          >
            {isCropMode ? 'Crop Mode ON' : 'Enable Crop'}
          </button>
          {cropData.w > 0 && cropData.h > 0 && (
            <div className="crop-info">
              <span style={{fontSize: '9px', color: '#94a3b8'}}>
                [{cropData.x}, {cropData.y}] {cropData.w}x{cropData.h}
              </span>
              <button onClick={handleApplyCrop} style={{marginTop: '5px'}}>
                Apply Crop
              </button>
            </div>
          )}
        </div>

        <div className="tool-unit">
          <span className="tool-label">Adjustments</span>
          <div className="slider-group">
            <div className="slider-item">
                <span>Bri({brightness})</span>
                <input type="range" min="-100" max="100" value={brightness} onChange={e => setBrightness(+e.target.value)} onMouseUp={() => processImage('brightness', { level: brightness })} />
            </div>
            <div className="slider-item">
                <span>Con({contrast})</span>
                <input type="range" min="-100" max="100" value={contrast} onChange={e => setContrast(+e.target.value)} onMouseUp={() => processImage('contrast', { level: contrast })} />
            </div>
          </div>
          <button 
            className="btn-primary" 
            style={{marginTop: '8px', width: '100%'}}
            onClick={() => {
              if (brightness !== 0) handleAddFilterAsLayer('brightness', { level: brightness });
              else if (contrast !== 0) handleAddFilterAsLayer('contrast', { level: contrast });
            }}
          >
            Add as Layer
          </button>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Filters</span>
          <div className="slider-group">
            <div className="slider-item">
                <span>Blur({blurIntensity})</span>
                <input type="range" min="0" max="20" value={blurIntensity} onChange={e => setBlurIntensity(+e.target.value)} onMouseUp={() => processImage('blur', { intensity: blurIntensity })} />
            </div>
            <div className="slider-item">
                <span>Sharp({sharpenIntensity})</span>
                <input type="range" min="0" max="20" value={sharpenIntensity} onChange={e => setSharpenIntensity(+e.target.value)} onMouseUp={() => processImage('sharpen', { intensity: sharpenIntensity })} />
            </div>
          </div>
          <button 
            className="btn-primary" 
            style={{marginTop: '8px', width: '100%'}}
            onClick={() => {
              if (blurIntensity !== 0) handleAddFilterAsLayer('blur', { intensity: blurIntensity });
              else if (sharpenIntensity !== 0) handleAddFilterAsLayer('sharpen', { intensity: sharpenIntensity });
            }}
          >
            Add as Layer
          </button>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Transform</span>
          <div className="input-group">
            <input type="number" className="number-input" value={rotateAngle} onChange={(e) => setRotateAngle(e.target.value)} />
            <button onClick={() => processImage('rotate', { angle: rotateAngle })}>Rotate</button>
          </div>
          <div className="btn-group" style={{marginTop:'5px'}}>
            <button onClick={() => processImage('flip/horizontal')}>Flip H</button>
            <button onClick={() => processImage('flip/vertical')}>Flip V</button>
          </div>
          <button 
            className="btn-primary" 
            style={{marginTop: '8px', width: '100%'}}
            onClick={handleAddGrayscaleLayer}
          >
            Add Grayscale Layer
          </button>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Zoom Result ({zoom}x)</span>
          <input type="range" min="1" max="4" step="1" value={zoom} onChange={e => setZoom(+e.target.value)} onMouseUp={() => processImage('zoom', { scale: zoom })} />
          <div className="btn-group" style={{marginTop:'5px'}}>
            <button className="btn-secondary" onClick={() => setLockedPos(null)}>Unlock</button>
            <button className="btn-secondary" onClick={handleResetAll}>Reset All</button>
          </div>
        </div>
      </nav>

      <main className="workspace-with-layers">
        <section className="panel-left">
          <MatrixOverlay linearData={sourceLinear} width={dimensions.width} height={dimensions.height} pos={lockedPos || mousePos} title="SOURCE" isLocked={!!lockedPos} />
          <MatrixOverlay 
            linearData={resultLinear || sourceLinear} 
            width={resultDimensions.width} 
            height={resultDimensions.height} 
            pos={
              (lockedPos || mousePos) 
                ? { 
                    x: (lockedPos ? lockedPos.x : mousePos.x) * (resultDimensions.width / dimensions.width), 
                    y: (lockedPos ? lockedPos.y : mousePos.y) * (resultDimensions.height / dimensions.height) 
                  } 
                : null
            } 
            title="RESULT" 
            isLocked={!!lockedPos} 
          />
          <MatrixOverlay 
            linearData={layerLinear || sourceLinear} 
            width={layerDimensions.width} 
            height={layerDimensions.height} 
            pos={
              (lockedPos || mousePos) 
                ? { 
                    x: (lockedPos ? lockedPos.x : mousePos.x) * (layerDimensions.width / dimensions.width), 
                    y: (lockedPos ? lockedPos.y : mousePos.y) * (layerDimensions.height / dimensions.height) 
                  } 
                : null
            } 
            title="LAYER VIEW" 
            isLocked={!!lockedPos} 
          />
        </section>

        <section className="preview-panel-three">
          <div className="comparison-view-three">
            <div className="viewport-third">
              <span className="view-tag">ORIGINAL</span>
              <div className="viewport-container" 
                   onClick={toggleLock} 
                   onMouseDown={handleMouseDown}
                   onMouseMove={(e) => handleMouseMoveViewport(e, false)} 
                   onMouseUp={() => setIsDragging(false)}
                   style={{cursor: isCropMode ? 'crosshair' : 'default'}}>
                {originalUrl && (
                  <img src={originalUrl} className="preview-img" draggable="false" alt="source" 
                       style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
                )}
                {isDragging && isCropMode && (
                  <div className="crop-selector-visual" style={{
                    position:'absolute', 
                    border:'2px dashed #6366f1', 
                    backgroundColor:'rgba(99,102,241,0.2)', 
                    left:cropData.x, 
                    top:cropData.y, 
                    width:cropData.w, 
                    height:cropData.h, 
                    pointerEvents:'none'
                  }} />
                )}
              </div>
            </div>
            
            <div className="viewport-third">
              <span className="view-tag">RESULT</span>
              <div className="viewport-container" onClick={toggleLock} onMouseMove={(e) => handleMouseMoveViewport(e, true)}>
                {previewUrl ? (
                  <img src={previewUrl} className="preview-img" draggable="false" alt="result" style={{ imageRendering: 'pixelated' }} />
                ) : (
                  originalUrl && <img src={originalUrl} className="preview-img" draggable="false" alt="source-copy" 
                                      style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
                )}
              </div>
            </div>

            <div className="viewport-third">
              <span className="view-tag">LAYER VIEW</span>
              <div className="viewport-container" onClick={toggleLock} onMouseMove={(e) => handleMouseMoveViewport(e, true)}>
                {layerViewUrl ? (
                  <img src={layerViewUrl} className="preview-img" draggable="false" alt="layers" style={{ imageRendering: 'pixelated' }} />
                ) : (
                  originalUrl && <img src={originalUrl} className="preview-img" draggable="false" alt="source-copy" 
                                      style={{ filter: isGrayscaleMode ? 'grayscale(100%)' : 'none', imageRendering: 'pixelated' }} />
                )}
              </div>
            </div>
          </div>
        </section>

        <section className="panel-right">
          <LayerCreator 
            onCreateLayer={handleCreateLayer}
            onAddImageLayer={handleAddImageLayer}
          />
          <LayerPanel 
            layers={layers}
            onToggleVisibility={handleToggleVisibility}
            onDeleteLayer={handleDeleteLayer}
            onMoveLayer={handleMoveLayer}
            onDuplicateLayer={handleDuplicateLayer}
            onOpacityChange={handleOpacityChange}
          />
        </section>
      </main>
    </div>
  );
}