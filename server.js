const express = require("express");
const fs = require("fs");
const app = express();

app.use((req,res,next)=>{
  res.setHeader("Access-Control-Allow-Origin","*");
  res.setHeader("Access-Control-Allow-Headers","Content-Type");
  res.setHeader("Access-Control-Allow-Methods","GET,POST,OPTIONS");
  if(req.method==="OPTIONS") return res.sendStatus(204);
  next();
});

app.use(express.json());
app.use(express.static("www"));

const KEY_FILE = process.env.HOME + "/.config/bijoy-ai/gemini.key";

app.post("/api/chat", async (req,res)=>{
  let timeout;
  try{
    const key=fs.readFileSync(KEY_FILE,"utf8").trim();
    const message=req.body.message;
    if(!message) return res.status(400).json({error:"মেসেজ লিখুন"});

    const controller=new AbortController();
    timeout=setTimeout(()=>controller.abort(),30000);

    const response=await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key="+encodeURIComponent(key),
      {
        method:"POST",
        headers:{"Content-Type":"application/json"},
        signal:controller.signal,
        body:JSON.stringify({
          systemInstruction:{parts:[{text:"তুমি BIJOY AI। সবসময় বাংলায় উত্তর দেবে এবং ব্যবহারকারীকে স্যার বলে সম্বোধন করবে। ফোনের কাজ সম্পন্ন হয়েছে বলে দাবি করবে না, যদি না তা সত্যিই করা হয়।"}]},
          contents:[{parts:[{text:message}]}]
        })
      }
    );
    const data=await response.json();
    if(!response.ok) return res.status(response.status).json({error:data.error?.message||"AI সংযোগে সমস্যা"});
    res.json({reply:data.candidates?.[0]?.content?.parts?.[0]?.text||"উত্তর পাওয়া যায়নি"});
  }catch(error){
    console.log("Server Error:",error.message);
    res.status(500).json({error:"সার্ভার সমস্যা: "+error.message});
  }finally{
    if(timeout) clearTimeout(timeout);
  }
});

app.listen(8081,"0.0.0.0",()=>console.log("BIJOY AI Server চলছে: 8081"));
