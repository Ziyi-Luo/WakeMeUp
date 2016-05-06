clear
a = csvread('4-21.csv')
fig = figure(1);
    plot(a(:,1),a(:,2)*20,'r','LineWidth',2)
    hold on
    plot(a(:,1),a(:,3),'Color','blue','LineWidth',1)
    hold on
    legend('Jawnbone data','Diff between images MSE');
    xlabel('Time');
    ylabel('Value');
    box off;