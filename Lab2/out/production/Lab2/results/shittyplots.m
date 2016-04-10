%Enligt PDF: 4, 5, 2 (Bäst, näst bäst, sämst)
clear

clf
epsilon = 0.0001;
%

d1 = importdata('diffArray1');
d2 = importdata('diffArray2');
d3 = importdata('diffArray3');
d4 = importdata('diffArray4');
d5 = importdata('diffArray5');

dw1 = importdata('diffArrayWorst1');
dw2 = importdata('diffArrayWorst2');
dw3 = importdata('diffArrayWorst3');
dw4 = importdata('diffArrayWorst4');
dw5 = importdata('diffArrayWorst5');

N = importdata('NArray');
exact = importdata('diffArrayExact');
%}

%{
system('java -Xmx2048m -cp .: pagerank.PageRank pagerank/linksDavis.txt');
dd1 = importdata('diffArray1');
dd2 = importdata('diffArray2');
dd3 = importdata('diffArray3');
dd4 = importdata('diffArray4');
dd5 = importdata('diffArray5');
count = 1
while(count <100)
    system('java -Xmx2048m -cp .: pagerank.PageRank pagerank/linksDavis.txt');
    dd1 = dd1 + importdata('diffArray1');
    dd2 = dd2 + importdata('diffArray2');
    dd3 = dd2 + importdata('diffArray3');
    dd4 = dd2 + importdata('diffArray4');
    dd5 = dd2 + importdata('diffArray5');
    count = count+1
end
d1 = dd1 / count;
d2 = dd2 / count;
d3 = dd3 / count;
d4 = dd4 / count;
d5 = dd5 / count;
%}

%{
smoothing = 10;
d1 = conv(d1, ones(1, smoothing)./smoothing, 'same');
d2 = conv(d2, ones(1, smoothing)./smoothing, 'same');
d3 = conv(d3, ones(1, smoothing)./smoothing, 'same');
d4 = conv(d4, ones(1, smoothing)./smoothing, 'same');
d5 = conv(d5, ones(1, smoothing)./smoothing, 'same');
dw1 = conv(dw1, ones(1, smoothing)./smoothing, 'same');
dw2 = conv(dw2, ones(1, smoothing)./smoothing, 'same');
dw3 = conv(dw3, ones(1, smoothing)./smoothing, 'same');
dw4 = conv(dw4, ones(1, smoothing)./smoothing, 'same');
dw5 = conv(dw5, ones(1, smoothing)./smoothing, 'same');
%}

%{
subplot(1, 2, 1);
semilogy(N,d1)
hold on;
semilogy(N,d2)
semilogy(N,d3)
semilogy(N,d4)
semilogy(N,d5)
%semilogy(N, exact);
semilogy([0, max(N)],[epsilon, epsilon], '--k')
legend('MC1', 'MC2', 'MC3', 'MC4', 'MC5', 'epsilon')
xlabel('N')
ylabel('Goodness 1')
axis([0 20 0 0.001]);

subplot(1, 2, 2);
semilogy(N,dw1)
hold on;
semilogy(N,dw2)
semilogy(N,dw3)
semilogy(N,dw4)
semilogy(N,dw5)
semilogy([-1000, max(N)],[epsilon, epsilon], '--k')
axis([0 20 0 max(d1)]);
legend('MC1', 'MC2', 'MC3', 'MC4', 'MC5', 'epsilon')
xlabel('N')
ylabel('Goodness 2')

%}

%
subplot(1,2,1)
plot(N,d1)
hold on;
plot(N,d2)
plot(N,d3)
plot(N,d4)
plot(N,d5)
axis([0 20 0 max(d1)/2]);
plot([-1000, max(N)],[epsilon, epsilon], '--k')
legend('MC1', 'MC2', 'MC3', 'MC4', 'MC5', 'epsilon')
xlabel('N')
ylabel('Goodness 1')

subplot(1, 2, 2);
plot(N,dw1)
hold on;
plot(N,dw2)
plot(N,dw3)
plot(N,dw4)
plot(N,dw5)
axis([0 20 0 max(dw1)/2]);
plot([-1000, max(N)],[epsilon, epsilon], '--k')
legend('MC1', 'MC2', 'MC3', 'MC4', 'MC5', 'epsilon')
xlabel('N')
ylabel('Goodness 2')
%}



