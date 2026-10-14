document.addEventListener('DOMContentLoaded',()=>{
    document.querySelectorAll('[data-admin-back]').forEach(link=>link.addEventListener('click',event=>{
        if(window.parent!==window){try{if(typeof window.parent.showDashboardOverview==='function'){event.preventDefault();window.parent.showDashboardOverview();}else if(typeof window.parent.closeDialog==='function'){event.preventDefault();window.parent.closeDialog();}}catch(error){}}
    }));
        document.addEventListener('click',event=>{
        const link=event.target.closest('a');if(!link||window.parent===window)return;
        const url=new URL(link.href,location.href);
        if(url.origin===location.origin&&url.pathname==='/admin/panel.php'){
            try{if(typeof window.parent.showDashboardOverview==='function'){event.preventDefault();window.parent.showDashboardOverview();}}catch(error){}
        }
    });
    if(document.body.dataset.adminPage==='account'){
        document.querySelectorAll('input[type=password].track').forEach(input=>{
            if(!/^p\d+$/.test(input.id))return;
            const wrap=document.createElement('div');wrap.className='password-control';input.before(wrap);wrap.append(input);
            const button=document.createElement('button');button.type='button';button.className='password-toggle';button.textContent='◉';button.setAttribute('aria-label','Hiện mật khẩu');button.title='Hiện mật khẩu';
            button.onclick=()=>{const show=input.type==='password';input.type=show?'text':'password';button.setAttribute('aria-label',show?'Ẩn mật khẩu':'Hiện mật khẩu');button.title=show?'Ẩn mật khẩu':'Hiện mật khẩu';};wrap.append(button);
        });
    }
});
