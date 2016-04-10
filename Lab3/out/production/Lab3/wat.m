clear all;
precision = @(v) sum(v)/size(v,2);
recall = @(v) sum(v)/10;
uniZombie = [1 1 1 1 0 1 0 0 0 0];
biZombie =  [1 1 1 1 1 1 1 1 1 0];

uniZombiePrec = [0 precision(uniZombie(1:3)) precision(uniZombie)];
uniZombieRecall = [0 recall(uniZombie(1:3)) recall(uniZombie)];

biZombiePrec = [0 precision(biZombie(1:3)) precision(biZombie)];
biZombieRecall = [0 recall(biZombie(1:3)) recall(biZombie)];

uniMoney = [1 1 0 0 1 0 0 1 0 0];
biMoney =  [1 1 0 0 1 1 0 1 1 0];

uniMoneyPrec = [0 precision(uniMoney(1:3)) precision(uniMoney)];
uniMoneyRecall = [0 recall(uniMoney(1:3)) recall(uniMoney)];

biMoneyPrec = [0 precision(biMoney(1:3)) precision(biMoney)];
biMoneyRecall = [0 recall(biMoney(1:3)) recall(biMoney)];

uniSeaFood = [1 0 1 1 0 1 1 1 0 0];
biSeaFood =  [1 1 0 1 1 0 0 0 1 0];

uniSeaFoodPrec = [0 precision(uniSeaFood(1:3)) precision(uniSeaFood)];
uniSeaFoodRecall = [0 recall(uniSeaFood(1:3)) recall(uniSeaFood)];

biSeaFoodPrec = [0 precision(biSeaFood(1:3)) precision(biSeaFood)];
biSeaFoodRecall = [0 recall(biSeaFood(1:3)) recall(biSeaFood)];

plot(uniZombieRecall, uniZombiePrec, 'b')
hold on;
plot(biZombieRecall, biZombiePrec, 'r');
axis([0 1 0 1.2]);
title('Zombie')
legend('Uni', 'bi');
xlabel('Recall');
ylabel('Precision');

figure;
plot(uniMoneyRecall, uniMoneyPrec, 'b')
hold on;
plot(biMoneyRecall, biMoneyPrec, 'r');
title('Money')
legend('Uni', 'bi');
axis([0 1 0 1.2]);
xlabel('Recall');
ylabel('Precision');

figure;
plot(uniSeaFoodRecall, uniSeaFoodPrec, 'b')
hold on;
plot(biSeaFoodRecall, biSeaFoodPrec, 'r');
title('SeaFood')
legend('Uni', 'bi');
axis([0 1 0 1.2]);
xlabel('Recall');
ylabel('Precision');

