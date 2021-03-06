package za.jay.blocks;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TitleScreenFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_title_screen, container, false);

        root.findViewById(R.id.title_screen_btn_timed).setOnClickListener(mButtonListener);
        root.findViewById(R.id.title_screen_btn_moves).setOnClickListener(mButtonListener);
        root.findViewById(R.id.title_screen_btn_endless).setOnClickListener(mButtonListener);

        return root;
    }

    private void launchGame(GameMode mode) {
        Intent intent = new Intent(getActivity(), GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_GAME_MODE, mode);
        getActivity().startActivity(intent);
    }

    private final View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch(view.getId()) {
                case R.id.title_screen_btn_timed:
                    launchGame(GameMode.TIMED);
                    break;
                case R.id.title_screen_btn_moves:
                    launchGame(GameMode.MOVES);
                    break;
                case R.id.title_screen_btn_endless:
                    launchGame(GameMode.ENDLESS);
                    break;
            }
        }
    };
}
