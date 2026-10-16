// Clockwork Scheduler - Interactive Client Logic

document.addEventListener('DOMContentLoaded', () => {
  initShowcaseTabs();
  initWorkflowSimulator();
  initCronEvaluator();
  initMetricsTicker();
  initApiTabs();
});

// 1. Showcase Screenshot Tabs
function initShowcaseTabs() {
  const tabs = document.querySelectorAll('.tab-pill');
  const img = document.getElementById('showcaseImage');
  const title = document.getElementById('showcaseTitle');

  const screenshots = {
    overview: {
      src: 'images/dashboard-overview.jpg',
      title: 'Real-Time Admin Dashboard: Job metrics, throughput gauges, active workers, and execution stream'
    },
    workflows: {
      src: 'images/workflows-pipeline.jpg',
      title: 'Spring State Machine Multi-Step Workflow Visualizer & Execution Pipeline'
    },
    dlq: {
      src: 'images/dlq-requeue.jpg',
      title: 'Dead Letter Queue (DLQ) Management, Exponential Backoff, and Single-Click Re-drive'
    }
  };

  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const key = tab.getAttribute('data-tab');
      if (screenshots[key] && img) {
        img.style.opacity = '0.3';
        setTimeout(() => {
          img.src = screenshots[key].src;
          if (title) title.textContent = screenshots[key].title;
          img.style.opacity = '1';
        }, 150);
      }
    });
  });
}

// 2. Interactive Workflow State Machine Simulator
function initWorkflowSimulator() {
  const btnRun = document.getElementById('btnRunWorkflow');
  const btnFail = document.getElementById('btnSimulateFailure');
  const terminal = document.getElementById('simTerminal');
  const steps = [
    { el: document.getElementById('step1'), tag: document.getElementById('stepTag1'), name: 'Generate Statement PDF' },
    { el: document.getElementById('step2'), tag: document.getElementById('stepTag2'), name: 'Email Statement to Customer' },
    { el: document.getElementById('step3'), tag: document.getElementById('stepTag3'), name: 'Archive to S3 Cold Storage' },
    { el: document.getElementById('step4'), tag: document.getElementById('stepTag4'), name: 'Notify Customer via SMS' }
  ];

  let isRunning = false;

  function log(msg, type = 'info') {
    if (!terminal) return;
    const now = new Date().toLocaleTimeString('en-GB');
    const line = document.createElement('div');
    line.className = 'log-line';
    line.innerHTML = `<span class="log-time">[${now}]</span> <span class="log-${type}">${msg}</span>`;
    terminal.appendChild(line);
    terminal.scrollTop = terminal.scrollHeight;
  }

  function resetSteps() {
    steps.forEach((s, idx) => {
      if (s.el) {
        s.el.className = 'sim-step';
        if (s.tag) {
          s.tag.textContent = 'PENDING';
          s.tag.className = 'step-status-tag';
        }
      }
    });
  }

  if (btnRun) {
    btnRun.addEventListener('click', async () => {
      if (isRunning) return;
      isRunning = true;
      btnRun.disabled = true;
      resetSteps();

      const wfId = 'wf-' + Math.random().toString(36).substring(2, 9);
      log(`Starting StateMachine instance: ${wfId}`, 'info');
      log(`Transitioning state: CREATED -> GENERATING`, 'info');

      // Step 1
      setStepState(0, 'active', 'RUNNING');
      log(`Step 1: Compiling Kenyan financial statement PDF...`, 'info');
      await delay(900);
      setStepState(0, 'completed', 'COMPLETED');
      log(`Step 1: PDF generated successfully (142,850 bytes)`, 'success');
      log(`Transitioning state: GENERATING -> EMAILING`, 'info');

      // Step 2
      setStepState(1, 'active', 'RUNNING');
      log(`Step 2: Dispatching encrypted email via SMTP...`, 'info');
      await delay(800);
      setStepState(1, 'completed', 'COMPLETED');
      log(`Step 2: Email sent with TLS verification to client@company.ke`, 'success');
      log(`Transitioning state: EMAILING -> ARCHIVING`, 'info');

      // Step 3
      setStepState(2, 'active', 'RUNNING');
      log(`Step 3: Uploading encrypted payload to MinIO/S3 cold storage...`, 'info');
      await delay(700);
      setStepState(2, 'completed', 'COMPLETED');
      log(`Step 3: Stored at s3://clockwork-archive/statements/${wfId}.pdf`, 'success');
      log(`Transitioning state: ARCHIVING -> COMPLETED`, 'info');

      // Step 4
      setStepState(3, 'active', 'RUNNING');
      log(`Step 4: Dispatching SMS delivery confirmation via Safaricom gateway...`, 'info');
      await delay(600);
      setStepState(3, 'completed', 'COMPLETED');
      log(`Step 4: SMS delivered to +254722123456`, 'success');

      log(`✓ Workflow ${wfId} completed in 3,024 ms across all steps!`, 'success');
      isRunning = false;
      btnRun.disabled = false;
    });
  }

  if (btnFail) {
    btnFail.addEventListener('click', async () => {
      if (isRunning) return;
      isRunning = true;
      btnFail.disabled = true;
      resetSteps();

      const wfId = 'wf-dlq-' + Math.random().toString(36).substring(2, 9);
      log(`Starting StateMachine with simulated fault at Step 2: ${wfId}`, 'warn');

      // Step 1 OK
      setStepState(0, 'active', 'RUNNING');
      await delay(700);
      setStepState(0, 'completed', 'COMPLETED');
      log(`Step 1: Statement compiled OK`, 'info');

      // Step 2 FAIL
      setStepState(1, 'active', 'RUNNING');
      log(`Step 2: Connecting to SMTP relay...`, 'info');
      await delay(900);
      setStepState(1, 'failed', 'FAILED');
      log(`Step 2: Connection timed out: smtp.company.ke:587`, 'err');
      log(`ApplyStateFilter intercepted failure: Exponential backoff attempt 1/3 scheduled in 2s`, 'warn');
      log(`Job execution moved to DEAD_LETTER queue for administrator inspection.`, 'err');

      isRunning = false;
      btnFail.disabled = false;
    });
  }

  function setStepState(index, stepClass, tagText) {
    const s = steps[index];
    if (s && s.el) {
      s.el.className = `sim-step ${stepClass}`;
      if (s.tag) s.tag.textContent = tagText;
    }
  }
}

// 3. Cron Expression Evaluator
function initCronEvaluator() {
  const select = document.getElementById('cronPresets');
  const input = document.getElementById('cronExpressionInput');
  const resultsList = document.getElementById('cronNextRuns');

  const presets = {
    mpesa: '0 3 * * *',
    sms: '0 9 * * 1-5',
    backup: '0 2 * * *',
    hourly: '0 * * * *'
  };

  function calculateNextRuns(expression) {
    if (!resultsList) return;
    resultsList.innerHTML = '';

    const now = new Date();
    const eatOffsetHours = 3; // Nairobi EAT is UTC+3

    for (let i = 1; i <= 5; i++) {
      const runDate = new Date(now.getTime() + i * 24 * 60 * 60 * 1000);
      if (expression.includes('3 * * *')) {
        runDate.setHours(3, 0, 0, 0);
      } else if (expression.includes('9 * *')) {
        runDate.setHours(9, 0, 0, 0);
      } else if (expression.includes('2 * *')) {
        runDate.setHours(2, 0, 0, 0);
      } else {
        runDate.setHours(now.getHours() + i, 0, 0, 0);
      }

      const li = document.createElement('li');
      li.innerHTML = `<strong>Run #${i}:</strong> ${runDate.toLocaleDateString('en-KE', { weekday: 'short', month: 'short', day: 'numeric' })} at ${runDate.toLocaleTimeString('en-KE', { hour: '2-digit', minute: '2-digit' })} EAT`;
      resultsList.appendChild(li);
    }
  }

  if (select && input) {
    select.addEventListener('change', () => {
      const val = select.value;
      if (presets[val]) {
        input.value = presets[val];
        calculateNextRuns(presets[val]);
      }
    });

    input.addEventListener('input', () => {
      calculateNextRuns(input.value);
    });

    calculateNextRuns(input.value || '0 3 * * *');
  }
}

// 4. Live Synthetic Metrics Ticker
function initMetricsTicker() {
  const counterRecon = document.getElementById('statRecon');
  const counterJobs = document.getElementById('statJobs');
  const counterRate = document.getElementById('statRate');

  let reconBase = 48500000;
  let jobsBase = 248920;

  setInterval(() => {
    // Tick stats smoothly
    reconBase += Math.floor(Math.random() * 5000 + 1500);
    jobsBase += 1;

    if (counterRecon) {
      counterRecon.textContent = 'KES ' + reconBase.toLocaleString('en-US');
    }
    if (counterJobs) {
      counterJobs.textContent = jobsBase.toLocaleString('en-US');
    }
  }, 3500);
}

// 5. REST API Curl Snippet Copier
function initApiTabs() {
  const copyButtons = document.querySelectorAll('.btn-copy-code');
  copyButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      const target = document.getElementById(targetId);
      if (target) {
        navigator.clipboard.writeText(target.textContent.trim()).then(() => {
          const original = btn.textContent;
          btn.textContent = 'Copied!';
          setTimeout(() => { btn.textContent = original; }, 1800);
        });
      }
    });
  });
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
